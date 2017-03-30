'use strict';

var titleMod = angular.module('mdwTitle', ['mdw']);

titleMod.factory('Title', ['$document', 'mdw', 'util', 'Node', 'DC',
                   function($document, mdw, util, Node, DC) {
  
  var Title = function(process) {
    Node.apply(this);
    this.process = process;
    this.workflowType = 'process';
    this.isTitle = true;
  };
  
  Title.prototype = new Node();
  
  Title.prototype.draw = function(diagram) {
    diagram.context.font = DC.TITLE_FONT;
    diagram.context.fillStyle = this.titleLinkHover ? DC.HYPERLINK_COLOR : DC.DEFAULT_COLOR;
    diagram.context.clearRect(this.display.x, this.display.y, this.display.w, this.display.h);
    diagram.context.fillText(this.process.name, this.display.x, this.display.y + this.display.h / 1.33);
    diagram.context.font = DC.DEFAULT_FONT;
    diagram.context.fillStyle = DC.DEFAULT_COLOR;
  };
  
  Title.prototype.prepareDisplay = function(diagram) {
    this.display = this.getDisplay(this.process.attributes.WORK_DISPLAY_INFO);
    diagram.context.font = DC.TITLE_FONT;
    var textMetrics = diagram.context.measureText(this.process.name);
    this.display.w = textMetrics.width;
    this.display.height = DC.TITLE_FONT_SIZE;
    var maxDisplay = { w: this.display.w + this.display.x, h: this.display.h + this.display.y };
    diagram.context.font = DC.DEFAULT_FONT;
    return maxDisplay;
  };
  
  Title.prototype.translate = function(deltaX, deltaY) {
    var x = this.display.x + deltaX;
    var y = this.display.y + deltaY;
    this.process.attributes.WORK_DISPLAY_INFO = this.getDisplayAttr(x, y, this.display.w, this.display.h); 
  };
  
  return Title;
}]);