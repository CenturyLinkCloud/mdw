// Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
'use strict';

var noteMod = angular.module('mdwNote', ['mdw']);

noteMod.factory('Note', ['$document', 'mdw', 'util',
                                function($document, mdw) {
  var Note = function(textNote) {
    this.textNote = textNote;
  };
  
  Note.DEFAULT_FONT_SIZE = 12;
  Note.BOX_OUTLINE_COLOR = 'yellow';  

  Note.prototype.draw = function(diagram) {
    diagram.drawBox(diagram.context, this.display.x, this.display.y, this.display.w, this.display.h, Note.BOX_OUTLINE_COLOR);
    if (this.textNote.content) {
      var lines = this.textNote.content.getLines();
      for (var i = 0; i < lines.length; i++) {
        diagram.context.fillText(lines[i], this.display.x + 4, this.display.y + 2 + Note.DEFAULT_FONT_SIZE * (i + 1));
      }
    }
  };
  
  Note.prototype.prepareDisplay = function(diagram) {
    var maxDisplay = { w: 0, h: 0 };
    this.display = diagram.getDisplay(this.textNote.attributes.WORK_DISPLAY_INFO);
    
    // boundaries
    if (this.display.x + this.display.w > maxDisplay.w)
      maxDisplay.w = this.display.x + this.display.w;
    if (this.display.y + this.display.h > maxDisplay.h)
      maxDisplay.h = this.display.y + this.display.h;
    
    return maxDisplay;
  };
  
  return Note;
}]);