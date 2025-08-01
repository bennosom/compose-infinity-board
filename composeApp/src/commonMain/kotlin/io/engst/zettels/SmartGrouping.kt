package io.engst.zettels

import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.add
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import kotlin.random.Random
import kotlin.random.nextULong

@Serializable
data class Position(val x: Int, val y: Int)

@Serializable
data class Size(val width: Int, val height: Int)

@Serializable
data class Item(
   val id: String = Random.nextULong().toString(),
   val description: String,
   val position: Position = Position(0,0),
   val size: Size = Size(100, 100),
   val groupId: String? = null // optional, Modell kann Gruppen vergeben
)

@Serializable
data class LayoutResult(val items: List<Item>)

object OpenAI {

   private const val MODEL = "gpt-4o-mini"
   private const val API_ENDPOINT = "https://api.openai.com/v1/chat/completions"
   private const val API_KEY = "insert your api key here"

   /**
    * Arrangiert Items semantisch (strukturiertes Ergebnis via Function Calling).
    */
   suspend fun arrangeItems(
      items: List<Item>,
      locale: String = "de"
   ): List<Item> {
      val client = getPlatform().httpClient()
      val tools = buildToolsSchema()
      val userPrompt = buildString {
         append("Ordne alle Objekte nach ihrer semantischen Zusammengehörigkeit räumlich an. ")
         append("Achte auf natürliche Cluster, ausreichenden Abstand zwischen Gruppen und lokale Ordnung innerhalb der Gruppe. ")
         append("Verschiebe nur die Positionen (x,y) – Größe bleibt gleich. ")
         append("Gib das Ergebnis ausschließlich über den Funktionsaufruf 'layout_objects' als JSON zurück.\n")
         append("Sprache: ").append(locale).append('\n')
         append("Input-Objekte (JSON):\n")
         append(Json.encodeToString(ListSerializer(Item.serializer()), items))
      }

      // Messages
      val messages = buildJsonArray {
         addJsonObject {
            put("role", "system")
            put(
               "content",
               "Du bist ein räumlicher Layouter. Du bildest semantische Cluster und platzierst Objekte kollisionsfrei."
            )
         }
         addJsonObject {
            put("role", "user")
            put("content", userPrompt)
         }
      }

      // Request-Body
      val body = buildJsonObject {
         put("model", MODEL)
         put("temperature", 0.2)
         put("messages", messages)
         put("tools", tools)
         // Erzwinge, dass das Modell die Function aufruft:
         putJsonObject("tool_choice") {
            put("type", "function")
            putJsonObject("function") { put("name", "layout_objects") }
         }
      }

      val response: ChatResponse = client.post(API_ENDPOINT) {
         header("Authorization", "Bearer $API_KEY")
         contentType(ContentType.Application.Json)
         setBody(body)
      }.body()

      val args = response.firstToolCallArgumentsOrNull()
         ?: error("Kein Function-Call erkannt – prüfe Model/Prompt.")

      // args = JSON-String der Funktionsargumente -> in LayoutResult parsen
      val result = Json.decodeFromString(LayoutResult.serializer(), args)
      return result.items
   }

   private fun buildToolsSchema(): JsonArray {
      // JSON Schema der Function-Parameter; Output (=Funktionsargument) nutzt die gleichen Data Classes
      val itemSchema = buildJsonObject {
         put("type", "object")
         putJsonObject("properties") {
            putJsonObject("id") {
               put("type", "string")
               put("description", "Eindeutige ID des Objekts")
            }
            putJsonObject("description") {
               put("type", "string")
               put("description", "Natürliche Beschreibung für semantische Nähe")
            }
            putJsonObject("position") {
               put("type", "object")
               putJsonObject("properties") {
                  putJsonObject("x") { put("type", "number") }
                  putJsonObject("y") { put("type", "number") }
               }
               putJsonArray("required") { add("x"); add("y") }
            }
            putJsonObject("size") {
               put("type", "object")
               putJsonObject("properties") {
                  putJsonObject("width") { put("type", "number") }
                  putJsonObject("height") { put("type", "number") }
               }
               putJsonArray("required") { add("width"); add("height") }
            }
            putJsonObject("groupId") {
               put("type", "string")
               put("description", "Optionale Cluster-ID, die semantische Gruppe markiert")
            }
         }
         putJsonArray("required") { add("id"); add("description"); add("position"); add("size") }
         put("additionalProperties", false)
      }

      val parameters = buildJsonObject {
         put("type", "object")
         putJsonObject("properties") {
            putJsonObject("items") {
               put("type", "array")
               put("items", itemSchema)
               put("description", "Liste der arrangierten Items; gleiche Struktur wie Input")
            }
         }
         putJsonArray("required") { add("items") }
         put("additionalProperties", false)
      }

      return buildJsonArray {
         addJsonObject {
            put("type", "function")
            putJsonObject("function") {
               put("name", "layout_objects")
               put(
                  "description",
                  "Gibt die arrangierten Items (mit aktualisierten Positionen und optionalen groupId) zurück."
               )
               put("parameters", parameters) // JSON Schema
            }
         }
      }
   }
}

/* --- Minimale DTOs zum Parsen der Chat API-Antwort --- */

@Serializable
data class ChatResponse(val choices: List<Choice>) {
   fun firstToolCallArgumentsOrNull(): String? =
      choices.firstOrNull()
         ?.message
         ?.toolCalls
         ?.firstOrNull()
         ?.function
         ?.arguments
}

@Serializable
data class Choice(val message: ChatMessage)

@Serializable
data class ChatMessage(
   @SerialName("tool_calls") val toolCalls: List<ToolCall>? = null
)

@Serializable
data class ToolCall(
   val type: String,
   val function: ToolFunction
)

@Serializable
data class ToolFunction(
   val name: String,
   val arguments: String // JSON-String der Funktionsargumente
)
