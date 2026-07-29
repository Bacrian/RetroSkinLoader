<img width="2048" height="722" alt="rsl banner" src="https://github.com/user-attachments/assets/0b11d74e-923e-4e9d-b1d4-de499d6d652e" />


Based on [CustomSkinLoader](https://modrinth.com/mod/customskinloader) and [SkinsRestorer](https://modrinth.com/plugin/skinsrestorer)
##  

**Made for Better Than Adventure (BTA) 8.0.1**

**SERVER AND CLIENT ARE REQUIRED TO HAVE THE MOD INSTALLED!**

##  


### What does it do?
Basically, upon opening a world or entering a server, the game asks Mojang's servers for a skin associated to your Minecraft name, so, if it doesn't find anything, you got Steve. So, this mods intercepts the call and searches in a different way, if Mojang doesn't find anything, it asks ElyBy.

### What if it doesn't find anything?
Lucky you, in that case we have commands:


```
/rsl skin (set|clear|list|admin)
```

RSL can also fetch skins from raw URLs, which means it isn't limited to a handful of providers, go crazy.

### How do commands work?

RSL works using a pool of skins determined by an admin in a server (or in your minecraft's or server's folder, search for _skins_list.json_ inside the '_RetroSkinLoader_' folder), which then, allow the players to select a skin from that pool.

**e.g.**
```
/rsl skin admin adduser hi notch

/rsl skin set hi
```

### Isn't it a bit complicated?
Absolutely, but hear me out, it works like a charm. It takes a while to get used to the commands, but it's really worth it, especially if you're playing with friends in offline servers.

_Also... It's been a long time since I modded Minecraft so... I'll try making it easier to use on the long run._


### Command list

_**/rsl skin set <id>**_ : Sets your skin to the selected id on the pool.

**_/rsl skin list_** : Shows every skin id available in the pool.

**_/rsl skin clear_** : Resets your skin to default.

**_/rsl skin admin add <id> <SkinURL> <(CapeURL | model)> <model>_** : Adds a skin to the pool via URL, use quotation marks on the URLs tho. **<model>** uses _slim_ or _default_.

**_/rsl skin admin adduser <id> <username>_** : Adds a skin to the pool via username.

**_/rsl skin admin remove <id>_** : Removes a skin from the pool.

### Can I add skin providers?
Of course you can, if it's compatible with the format on RSLconfig.json, go nuts.
