'use strict';

var selectionMod = angular.module('mdwSelection', ['mdw']);

// selected object(s)
selectionMod.factory('Selection', ['mdw', function(mdw) {
  var Selection = function(diagram) {
    this.diagram = diagram;
    this.selectObjs = [];
  };
  
  Selection.prototype.includes = function(obj) {
    for (var i = 0; i < this.selectObjs.length; i++) {
      if (this.selectObjs[i] === obj)
        return true;
    }
    return false;
  };
  
  Selection.prototype.isMulti = function() {
    return this.selectObjs.length > 1;
  };
  
  Selection.prototype.getSelectObj = function() {
    if (this.selectObjs.length === 0)
      return null;
    else
      return this.selectObjs[0];
  };
  
  Selection.prototype.setSelectObj = function(obj) {
    this.selectObjs = obj ? [obj] : [];
  };
  
  Selection.prototype.add = function(obj) {
    if (!this.includes(obj)) {
      this.selectObjs.push(obj);
      if (obj.isStep) {
        // add any contained links
        var stepLinks;
        let step = this.diagram.getStep(obj.activity.id);
        if (step) {
          stepLinks = this.diagram.getLinks(obj);
        }
        else {
          for (let i = 0; i < this.diagram.subflows.length; i++) {
            let subflow = this.diagram.subflows[i];
            let step = subflow.getStep(obj.activity.id);
            stepLinks = subflow.getLinks(obj);
            if (stepLinks)
              break;
          }
        }
  
        if (stepLinks) {
          for (let i = 0; i < stepLinks.length ; i++) {
            var stepLink = stepLinks[i];
            if (stepLink.from === obj) {
              if (this.includes(stepLink.to)) {
                this.add(stepLink);
                stepLink.select();
              }
            }
            else {
              if (this.includes(stepLink.from)) {
                this.add(stepLink);
                stepLink.select();
              }
            }
          }
        }
      }
    }
  };
  
  Selection.prototype.remove = function(obj) {
    var newSel = [];
    for (var i = 0; i < this.selectObjs.length; i++) {
      if (this.selectObjs[i] !== obj)
        newSel.push(this.selectObjs[i]);
    }
    this.selectObjs = newSel;
  };
  
  Selection.prototype.doDelete = function() {
    for (var i = 0; i < this.selectObjs.length; i++) {
      var selObj = this.selectObjs[i];
      if (selObj.isStep)
        this.diagram.deleteStep(selObj);
      else if (selObj.isLink)
        this.diagram.deleteLink(selObj);
      else if (selObj.isSubflow)
        this.diagram.deleteSubflow(selObj);
      else if (selObj.isNote)
        this.diagram.deleteNote(selObj);
    }
  };
  
  // works for the primary (single) selection to reenable anchors
  Selection.prototype.reselect = function() {
    if (this.getSelectObj() && !this.isMulti()) {
      var selObj = this.getSelectObj();
      var id = selObj.workflowItem ? selObj.workflowItem.id : null;
      if (id && typeof id === 'string') {
        this.setSelectObj(this.diagram.get(id));
        if (!this.getSelectObj()) {
          for (var i = 0; i < this.diagram.subflows.length; i++) {
            this.setSelectObj(this.diagram.subflows[i].get(id));
            if (this.getSelectObj())
              break;
          }
        }
      }
      if (!this.getSelectObj()) {
        this.setSelectObj(this.diagram.label);
      }
    }
  };
  
  Selection.prototype.move = function(startX, startY, deltaX, deltaY) {
    var selection = this;
    
    if (!this.isMulti() && this.getSelectObj().isLink) {
      // move link label
      var link = this.getSelectObj();
      if (link.label && link.label.isHover(startX, startY)) {
        link.moveLabel(deltaX, deltaY);
      }
    }
    else {
      for (let i = 0; i < this.selectObjs.length; i++) {
        let selObj = this.selectObjs[i];
        if (selObj.isStep) {
          let step = this.diagram.getStep(selObj.activity.id);
          if (step) {
            selObj.move(deltaX, deltaY);
            let links = this.diagram.getLinks(step);
            for (let j = 0; j < links.length; j++) {
              if (!this.includes(links[j]))
                links[j].recalc(step);
            }
          }
          else {
            // try subflows
            for (let j = 0; j < this.diagram.subflows.length; j++) {
              let subflow = this.diagram.subflows[j];
              let step = subflow.getStep(selObj.activity.id);
              if (step) {
                // only within bounds of subflow
                selObj.move(deltaX, deltaY, subflow.display);
                let links = subflow.getLinks(step);
                for (let k = 0; k < links.length; k++) {
                  if (!this.includes(links[k]))
                    links[k].recalc(step);
                }
              }
            }
          }
        }
        else {
          // TODO: prevent subproc links in multisel from moving beyond border
          selObj.move(deltaX, deltaY);
        }
      }
    }
    
    this.diagram.draw();
    
    for (let i = 0; i < this.selectObjs.length; i++) {
      let selObj = this.selectObjs[i];
      let reselObj = this.find(selObj);
      if (reselObj) {
        reselObj.select();
      }
    }
    // TODO: diagram label loses select
  };
  
  // re-find the selected object after it's been moved
  Selection.prototype.find = function(obj) {
    if (obj.workflowItem && obj.workflowItem.id) {
      var found = this.diagram.get(obj.workflowItem.id);
      if (found)
        return found;
      
      // try subflows
      for (let i = 0; i < this.diagram.subflows.length; i++) {
        let subflow = this.diagram.subflows[i];
        found = subflow.get(obj.workflowItem.id);
        if (found)
          return found;
      }
    }    
  };

  return Selection;
}]);
  