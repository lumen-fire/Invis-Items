# Invis Items
### **[WIP]** 

**_Requires [PacketEvents](https://modrinth.com/project/HYKaKraK)_**  
**_Supports 1.21.6+ spigot, paper, and (most) paper forks_**

A simple 1.21.6+ spigot & paper plugin to make items invisible to all but those holding them.
Useful for lobby items, minigames, trolling, or really any time when you need to keep an item hidden. 

## Features
- Items are securely hidden from players by using packet level interception – clients can't possibly know about items they were never told of.
- Supports applying and removing invisibility to and from items, as well as giving invisible items and replacing slots with invisible items.
- Permissions down to each subcommand node and customisable messages in config.yml.
- Brigadier (minecraft's native command system) support on papermc (and papermc forks) for command colouration, /execute as support, argument tooltips, and more of brigadier's advantages. 
- Supports selectors like @p, @r, etc, in commands and offers full tab complete.
- Should support Purpur and most other forks of paper.

## Command and Permissions

| Subcommand                                          | Permission         | Description                                                                              |
|:----------------------------------------------------|:-------------------|:-----------------------------------------------------------------------------------------|
| /invisitems version                                 | invisitems.version | Send the plugin version in chat                                                          |
| /invisitems reload                                  | invisitems.reload  | Reload the config.yml/custom messages                                                    |
| /invisitems apply \[\<slot>] \[\<entities>]         | invisitems.apply   | Apply invisibility to the item in the hand or specified slot of you or other entities    |
| /invisitems remove \[\<slot>] \[\<entities>]        | invisitems.remove  | Remove invisibility from the item in the hand or specified slot of you or other entities |
| /invisitems give \<item> \[\<amount>] \[\<players>] | invisitems.give    | Give an invisible item or invisible items to yourself or other players                   |
| /invisitems replace \<slot> \<item> \[\<entities>]  | invisitems.replace | Replace the specified slot with an invisible item, for you or other entities             |

## Config.yml
The config.yml contains the customisable messages. Use ```/invisitems reload``` to reload it.
```yaml
messages:
  reload: "Reloaded InvisItems"
  version: "Running InvisItems version %version%"
  receive-item: "You have been given %amount%x invisible %item%"
  give-player-item: "Gave %amount%x invisible %item% to %player%"
  apply-to-item: "Applied invisibility to %item%"
  apply-to-entities: "Applied invisibility to slot %slot% for %amount% entity(s)"
  remove-from-item: "Removed invisibility from %item%"
  remove-from-entities: "Removed invisibility from slot %slot% for %amount% entity(s)"
  set-slot-to-item: "Set slot %slot% to invisible %item%"
  set-entity-slot-to-item: "Set slot %slot% to invisible %item% for %amount% entity(s)"
```