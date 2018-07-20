'use strict';

var noteMod = angular.module('mdwNote', ['mdw']);

noteMod.factory('Note', ['$document', 'mdw', 'util', 'Shape', 'DC',
                         function($document, mdw, util, Shape, DC) {
  var Note = function(diagram, textNote) {
    Shape.call(this, diagram, textNote);
    this.diagram = diagram;
    this.textNote = textNote;
    this.workflowType = 'textNote';
    this.isNote = true;
  };
  
  Note.prototype = new Shape();
  
  Note.BOX_FILL_COLOR = '#ffc';
  Note.BOX_OUTLINE_COLOR = 'gray';
  Note.BOX_ROUNDING_RADIUS = 2;
  Note.FONT_SIZE= 13;
  Note.FONT= '13px monospace';

  Note.create = function(diagram, idNum, x, y) {
    var textNote = Note.newTextNote(diagram, idNum, x, y);
    var note = new Note(diagram, textNote);
    var disp = note.getDisplay();
    note.display = {x: disp.x, y: disp.y, w: disp.w, h: disp.h};
    return note;
  };
  
  Note.newTextNote = function(diagram, idNum, x, y) {
    var w = 200;
    var h = 60;
    var textNote = {
        id: 'N' + idNum,
        content: '',
        attributes: {WORK_DISPLAY_INFO: 'x=' + x + ',y=' + y + ',w=' + w + ',h=' + h},
        transitions: []
    };
    return textNote;
  };
  
  Note.prototype.draw = function() {
    this.diagram.rect(this.display.x, this.display.y, this.display.w, this.display.h, Note.BOX_OUTLINE_COLOR, Note.BOX_FILL_COLOR, Note.BOX_ROUNDING_RADIUS);
    if (this.textNote.content) {
      var lines = this.textNote.content.getLines();
      this.diagram.context.font = Note.FONT;
      for (var i = 0; i < lines.length; i++) {
        this.diagram.context.fillText(lines[i], this.display.x + 4, this.display.y + 2 + Note.FONT_SIZE * (i + 1));
      }
    }
  };
  
  Note.prototype.prepareDisplay = function() {
    var maxDisplay = { w: 0, h: 0 };
    this.display = this.getDisplay();
    
    // boundaries
    if (this.display.x + this.display.w > maxDisplay.w)
      maxDisplay.w = this.display.x + this.display.w;
    if (this.display.y + this.display.h > maxDisplay.h)
      maxDisplay.h = this.display.y + this.display.h;
    
    return maxDisplay;
  };
  
  Note.prototype.move = function(deltaX, deltaY) {
    var x = this.display.x + deltaX;
    var y = this.display.y + deltaY;
    this.setDisplayAttr(x, y, this.display.w, this.display.h);
    return true;
  };
  
  Note.prototype.resize = function(x, y, deltaX, deltaY) {
    var display = this.resizeDisplay(x, y, deltaX, deltaY, Shape.MIN_SIZE);
    this.setDisplayAttr(display.x, display.y, display.w, display.h);
  };
  
  return Note;
}]);