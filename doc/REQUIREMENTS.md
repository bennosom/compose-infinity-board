# Project goal
The goal of this project is to build a multiplatform app with Kotlin targeting at least Android and iOS.

## Main purpose
The purpose of the app is to take notes in a structured way.

## User stories
- As a user I want to take notes on a infinite whiteboard
- As a user I want to arrange notes in a free manner

## Functional requirements
- The user should be able to pan and zoom the board (aka "board gestures").
- The user should be able to drag a note individually after long-press.
- Board gestures should have priority over all other gestures including child gestures. 
- If the board is being panned or zoomed by two pointers and one pointer gets up, then continue panning the board.
 