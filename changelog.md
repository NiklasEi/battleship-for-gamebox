### v 3.1.0
- compatibility with minecraft 1.13
- clean up material loading

### v3.0.0
- update to GameBox v2
- mavenize the repository

# 

### v2.3.3
- improve default lang
- add 'waiting' title to language file

### v2.3.2
- check inventory title length if GB flag is set

### v 2.3.1
- fix error caused by initializing the GameManager in reload() before checking whether GameBox is enabled
- fix usage of getClickedInventory for CraftBukkit

### v 2.3.0
- push GB dep to 1.5.0
  - now compatible with /gba reload

### v 2.2.0
- centralised more code to GameBox
  - use static main-key from GUIManager (GameBox) for guis
  - chat color in Main class
  - Sounds
  - use ItemStackUtil from GameBox to load ItemStacks
- removed deprecated methods and variables (now depends on GB version 1.3.0)
- replace all non numbers in gb version
- add mandarin lang file
- add spanish lang file
