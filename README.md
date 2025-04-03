# Lucky Clan Bingo Plugin

This plugin was created to make drop verification during Bingo events easier for Lucky cc.  
</br>  
</br>
  
# Features
This plugin works roughly the same as your standard Discord Loot Logger plugin, with 1 key difference: the list of items is predetermined, and cannot be altered by users.
  
### Easy drops submissions
The Lucky Clan Bingo plugin features the ability to submit drops via Discord Webhooks. These webhooks can be set up in advance in your teams' submission channels, and prevents having to faff around with a passphrase for RuneLite-using participants.
Unfortunately this remains to be the case for Mobile playing participants.
  
### Anti-cheat
By only accepting submissions sent through the webhook, event organisers can be assured the submitted drops were gained during the time of the event, and they do not have to scan the screenshots for passphrases.
Besides this, in certain events, participants could choose to not submit a drop they received as it would not benefit them, but would benefit their opponents. This would be the case in an event organised using the example event resources later on in this document.
  
### Item List
[The items list can be found in the Plugin repository, found by clicking here](https://github.com/EwwItsMike/LuckyClanBingo/blob/master/src/main/resources/items.txt)  
While this list of items is quite extensive at 433 items at the time of writing, it is possible some potential Bingo tile items were missed. If you wish to get items added to the list, [please create a ticket in the Issues pages of the plugin's repository.](https://github.com/EwwItsMike/LuckyClanBingo/issues)  
I will add the items as soon as possible.  

</br>  
</br>  
</br>  
</br>  
</br>  
</br>  
</br>  
</br>  

# Example event resources
The following are Google Sheet documents initially used for a Lucky cc Bingo event. To demonstrate the use of the plugin, and give other clans and communities some resources to get started on their own events.  

[Participant template sheet](https://docs.google.com/spreadsheets/d/17YPfy7IFr2w4XRGgF7oGEOrmhYDv_lUiDqBz-REXmlM/edit?usp=sharing)  
[Tiles master sheet](https://docs.google.com/spreadsheets/d/1qTFFCE7YD2hnktdj5iPAjazOLsdTFJe5Yg2O8suA0qY/edit?usp=sharing)  

To recreate the same event for your own community, start by opening both the above links, and make a copy to your own Google Drive by clicking File -> Make a copy.  

</br>  

## Event overview
This bingo event features a fairly standard 6x6 Blackout bingo board.  
The workings of this bingo are based on how an actual IRL bingo would work: every participant has their own, randomised and unique board. Every time a number is drawn (in this case, a drop received), the tile is marked as completed for every participant.  
</br>  

## Tiles master sheet
This spreadsheet is very simple. It lists the names of tiles, has a description field for clarification of the tiles if needed, it holds the links for images of the tiles, and has checkboxes to mark the tiles' completion status.  
  
To add or remove potential tiles to the event, organisers only need to edit this spreadsheet. You may delete rows you don't want in your event, or add new ones at the bottom.  Please remember to not leave empty rows in the list, but rather delete the entire row by right-clicking on the row number on the left.  
  
After adding new potential tiles, select a cell in the D column, labelled "New Image URL". Click and drag the orb in the bottom right corner of the cell down to where your new tile additions end. Do the same for the checkboxes in the F column, labelled "Completed".  
All that's left now, is to paste an image URL into the E column labelled "Image".  
  
Your event organisers and "bingo judges" should receive Edit access to this sheet. When a drop is received by a participant and is sent to the Webhook, your organisers or judges simply check it off in the Completed column.  
</br>

## Participant template sheet
This spreadsheet functions as the template for the spreadsheets your participants will receive. **PLEASE MAKE SURE EVERYTHING IS WORKING AS INTENDED IN THIS SHEET BEFORE CREATING COPIES FOR YOUR PARTICIPANTS!**  
  
- First, click the 3 lines button at the bottom of the sheet, and open the hidden "TilesList" sheet. This sheet imports the list of potential tiles from your Tiles master sheet. 
- In your Tiles master sheet, press the Share button on the top right, and copy the link.
- In cell A1 of the TilesList sheet of your Participant Template is a formula called IMPORTRANGE. Edit the URL in this formula to the link you just copied, and remove the "edit?usp=sharing" part at the end of the url.
- From this same sheet, copy cell A2 to C-whatever your final row is. Copy it by pressing Ctrl+C.
- Click on the 3 lines button at the bottom of the sheet again, and open the hidden RandomTiles sheet.
- Ctrl+A everything in this sheet, and Backspace or Delete to remove it. 
- Select cell A1 and Ctrl+V to paste your new tiles list into it.
- Hide both the TilesList and RandomTiles sheets again by right-clicking them at the bottom, and pressing Hide sheet.
  
The hidden "Scoring" sheet handles scoring for the participant sheet. It can be a bit disorientating, so I advise you not to try to edit this sheet unless you know what you are doing.  
</br>

## Participant sheets
Now with the Tiles master spreadsheet and Participant template sheet set up, it is time to create the sheets for your participants.  
  
- Go into your Google Drive folder, and create a new folder for the Participants sheets. We do this to keep things relatively organised.
- Click the 3 dots button on the Template sheet in your Google Drive folder, and create a copy. Drag this copy into the Participants folder.
- Rename the new Participant sheet for organising purposes. I advise to name it "[NAME] 's sheet" where NAME is the participant's name.
- Edit the cell that says [Name] in the Board sheet to personalise it for your participant.
- Through the 3 lines button, open the RandomTiles sheet again. 
- Ctrl+A to select everything. Rightclick any cell in the selected range, open the submenu at the bottom labelled "View more cell actions", and select "Randomise range".
- Hide the RandomTiles sheet again by rightclicking it in the bottom bar, and clicking "Hide sheet".
- Your participant's sheet is now randomised, and the sheet takes care of everything visually!
- Press "Share" in the top right, and give your participant **VIEWER** permissions. With editing permissions they have the possibility to edit their own board, so I advise against that.  
</br>
  
## Discord Webhook & Plugin setup
All that is left now is to set up the Discord Webhook and the Lucky Clan Bingo plugin up to be used by your participants!  
  
- First, your participants should already have installed the plugin from the Plugin Hub interface within RuneLite.
- Go into your Discord server, and select "Edit channel" on the Text channel you wish to have the plugin send drop submissions to.
- Select "Integrations" in the menu on the left, and press the "Create Webhook" button.
- Select the newly created Webhook, and edit its name and image if you wish.
- Press the "Copy Webhook URL" button.
- This URL will have to be sent to your participants. 
- Your participants can go into the Plugin configuration tab on the sidebar of RuneLite. Find the Lucky Clan Bingo plugin in there, and click the cogwheel for the configuration settings.
- Paste the Webhook URL in the configuration field labelled "Webhook link". That's it!

While the original event was intended for Solo participants, you may quite easily adept it for a Teams-based event by setting up a single Participants sheet for a team.
