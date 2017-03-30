'use strict';

var noteMod = angular.module('mdwNote', ['mdw']);

noteMod.factory('Note', ['$document', 'mdw', 'util', 'Node', 'DC',
                         function($document, mdw, util, Node, DC) {
  var Note = function(textNote) {
    Node.apply(this);
    this.textNote = textNote;
    this.workflowType = 'textNote';
    this.isNote = true;
  };
  
  Note.prototype = new Node();
  
  Note.BOX_OUTLINE_COLOR = 'yellow';
  Note.FONT_SIZE= 13;
  Note.FONT= '13px monospace';

  Note.prototype.draw = function(diagram) {
    diagram.rect(this.display.x, this.display.y, this.display.w, this.display.h, Note.BOX_OUTLINE_COLOR);
    if (this.textNote.content) {
      var lines = this.textNote.content.getLines();
      diagram.context.font = Note.FONT;
      for (var i = 0; i < lines.length; i++) {
        diagram.context.fillText(lines[i], this.display.x + 4, this.display.y + 2 + Note.FONT_SIZE * (i + 1));
      }
    }
  };
  
  Note.prototype.prepareDisplay = function(diagram) {
    var maxDisplay = { w: 0, h: 0 };
    this.display = this.getDisplay(this.textNote.attributes.WORK_DISPLAY_INFO);
    
    // boundaries
    if (this.display.x + this.display.w > maxDisplay.w)
      maxDisplay.w = this.display.x + this.display.w;
    if (this.display.y + this.display.h > maxDisplay.h)
      maxDisplay.h = this.display.y + this.display.h;
    
    return maxDisplay;
  };
  
  return Note;
}]);