# Interactive Wrestling Brackets

Desktop Swing app for building and managing wrestling brackets, specifically for the NCAA Championships.

## Requirements
- Java 17
- Maven 3.9+

## Build
```bash
mvn clean package
```

The runnable JAR is created at:
`target/interactive-wrestling-brackets-1.0-SNAPSHOT.jar`

## Run
```bash
java -jar target/interactive-wrestling-brackets-1.0-SNAPSHOT.jar
```

## Native Installer (Windows)
Requires a full JDK 17+ with `jpackage` available on PATH.

PowerShell helper:
```powershell
.\scripts\build-installer.ps1
```

Optional installer type:
```powershell
.\scripts\build-installer.ps1 -Type exe
```

The installer will be written to `dist/`.

Manual `jpackage` equivalent:
```powershell
jpackage --type msi --input target --dest dist --name "InteractiveWrestlingBrackets" --main-jar "interactive-wrestling-brackets-1.0-SNAPSHOT.jar" --main-class "bracket.Bracket" --app-version "1.0.0" --add-modules "java.desktop"
```

## Notes
- The JAR is built with a manifest `Main-Class` pointing to `bracket.Bracket`.
- The `target/` directory is ignored by git; ship built artifacts via GitHub Releases instead of committing them.
