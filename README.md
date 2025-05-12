# BetaPig Minecraft Launcher

A custom Minecraft launcher for BetaPig server running Minecraft Beta 1.7.3.

## Features

- Modern UI with dark theme
- Username login
- Optimine and Optifog support
- Skin upload functionality
- Direct connection to BetaPig server
- News display

## Requirements

- Java 8 or higher
- Maven for building

## Building

1. Clone the repository
2. Run `mvn clean package`
3. The launcher will be built as `target/minecraft-launcher-1.0-SNAPSHOT.jar`

## Usage

1. Run the launcher using `java -jar minecraft-launcher-1.0-SNAPSHOT.jar`
2. Enter your username
3. Optionally enable Optimine and/or Optifog
4. Click "Launch" to start the game

## Notes

- The launcher will automatically create a `.betapig` directory in your home folder
- Game files will be downloaded automatically on first launch
- Skins are stored locally in the `.betapig/skins` directory
