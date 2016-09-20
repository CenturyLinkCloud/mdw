// Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
'use strict';

var linkMod = angular.module('mdwLink', ['mdw']);

linkMod.factory('Link', ['mdw', 'util', function(mdw, util) {

  var Link = function(transition, from, to) {
    this.transition = transition;
    this.from = from;
    this.to = to;
  };
  
  Link.DEFAULT_COLOR = 'black';
  Link.DEFAULT_FONT_SIZE = 12;
  Link.CR = 8;
  Link.LINK_WIDTH = 3;

  Link.LINK_TYPES = {
    STRAIGHT: 'Straight',
    ELBOW: 'Elbow',
    CURVE: 'Curve',
    ELBOWH: 'ElbowH',
    ELBOWV: 'ElbowV'      
  };
  
  Link.AUTO_ELBOW_LINK_TYPES = {
    AUTOLINK_H: 1,
    AUTOLINK_V: 2,
    AUTOLINK_HV: 3,
    AUTOLINK_VH: 4,
    AUTOLINK_HVH: 5,
    AUTOLINK_VHV: 6      
  };
  
  Link.ELBOW_THRESHOLD = 0.8;
  Link.ELBOW_VH_THRESHOLD = 60;
  
  Link.prototype.draw = function(diagram) {
    
    diagram.context.strokeStyle = this.getEventColor(this.transition.event);
    this.drawConnector(diagram.context);
    diagram.context.strokeStyle = Link.DEFAULT_COLOR;
    // todo draw shape

    // title
    diagram.context.fillStyle = this.getEventColor(this.transition.event);
    diagram.context.fillText(this.title.text, this.title.x, this.title.y);
    diagram.context.fillStyle = Link.DEFAULT_COLOR;
    
  };
  
  // sets display/title and returns an object with w and h for required size
  Link.prototype.prepareDisplay = function(diagram) {
    var maxDisplay = { w: 0, h: 0};
    this.display = this.getDisplay(this.transition.attributes.TRANSITION_DISPLAY_INFO);
    // TODO determine effect on maxDisplay
    
    // title
    var label = this.transition.event === 'FINISH' ? '' : this.transition.event + ':';
    label += this.transition.resultCode ? this.transition.resultCode : ''; 
    this.title = { text: label };
    // TODO title coords
    this.title.x = this.display.lx;
    this.title.y = this.display.ly + Link.DEFAULT_FONT_SIZE;
    
    return maxDisplay;
  };
  
  Link.prototype.getDisplay = function(displayAttr) {
    var display = {};
    if (displayAttr) {
      var vals = displayAttr.split(',');
      display.xs = [];
      display.ys = [];
      vals.forEach(function(val) {
        if (val.startsWith('lx='))
          display.lx = parseInt(val.substring(3));
        else if (val.startsWith('ly='))
          display.ly = parseInt(val.substring(3));
        else if (val.startsWith('xs=')) {
          val.substring(3).split('&').forEach(function(x) {
            display.xs.push(parseInt(x));
          });
        }
        else if (val.startsWith('ys=')) {
          val.substring(3).split('&').forEach(function(y) {
            display.ys.push(parseInt(y));
          });
        }
        else if (val.startsWith('type='))
          display.type = val.substring(5);
      });
    }
    return display;
  };
  
  Link.prototype.drawConnector = function(context) {
    var type = this.display.type;
    var xs = this.display.xs;
    var ys = this.display.ys;
    
    var previousLineWidth = context.lineWidth;
    context.lineWidth = Link.LINK_WIDTH;
    if (!type || type.startsWith('Elbow')) {
      if (xs.length == 2) {
        this.drawAutoElbowConnector(context);
      }
      else {
        context.beginPath();
        var horizontal = ys[0] == ys[1] && (xs[0] != xs[1] || xs[1] == xs[2]);
        context.moveTo(xs[0], ys[0]);
        for (var i = 1; i < xs.length; i++) {
          if (horizontal) {
            context.lineTo(xs[i] > xs[i-1] ? xs[i] - Link.CR : xs[i] + Link.CR, ys[i]);
            if (i < xs.length - 1)
              context.quadraticCurveTo(xs[i], ys[i], xs[i], ys[i+1] > ys[i] ? ys[i] + Link.CR : ys[i] - Link.CR);
          }
          else {
              context.lineTo(xs[i], ys[i] > ys[i - 1] ? ys[i] - Link.CR : ys[i] + Link.CR);
              if ( i <xs.length - 1)
                context.quadraticCurveTo(xs[i], ys[i], xs[i + 1] > xs[i] ? xs[i] + Link.CR : xs[i] - Link.CR, ys[i]);
          }
          horizontal = !horizontal;
        }
        context.stroke();
      }
    }
    else if (type == Link.LINK_TYPES.CURVE) {
      // TODO: curve logic from Link.java determineShape() method
      if (xs.length == 4) {

      }
      else if (xs.length == 3) {
        
      }
      else if (this.from === this.to) {
        
      }
      else {
        
      }
    }
    else {
      
    }
    context.lineWidth = previousLineWidth;
    
    // TODO draw arrow
  };
  
  Link.prototype.drawAutoElbowConnector = function(context) {
    var xs = this.display.xs;
    var ys = this.display.ys;
    var t;
    switch (this.getAutoElbowLinkType()) {
      case Link.AUTOLINK_V:
      case Link.AUTOLINK_H:
        context.beginPath();
        context.moveTo(xs[0], ys[0]);
        context.lineTo(xs[1], ys[1]);
        context.stroke();
        break;
      case Link.AUTOLINK_VHV:
        context.beginPath();
        t = (ys[0] + ys[1]) / 2;
        context.moveTo(xs[0], ys[0]);
        context.lineTo(xs[0], t > ys[0] ? t - Link.CR : t + Link.CR);
        context.quadraticCurveTo(xs[0], t, xs[1] > xs[0] ? xs[0] + Link.CR : xs[0] - Link.CR, t);
        context.lineTo(xs[1] > xs[0] ? xs[1] - Link.CR : xs[1] + Link.CR, t);
        context.quadraticCurveTo(xs[1], t, xs[1], ys[1] > t ? t + Link.CR : t-Link.CR);
        context.lineTo(xs[1], ys[1]);
        context.stroke();
        break;
      case Link.AUTOLINK_HVH:
        context.beginPath();
        t = (xs[0] + xs[1]) / 2;
        context.moveTo(xs[0], ys[0]);
        context.lineTo(t > xs[0] ? t - Link.CR : t + Link.CR, ys[0]);
        context.quadraticCurveTo(t, ys[0], t, ys[1] > ys[0] ? ys[0] + Link.CR : ys[0] - Link.CR);
        context.lineTo(t, ys[1] > ys[0] ? ys[1] - Link.CR : ys[1] + Link.CR);
        context.quadraticCurveTo(t, ys[1], xs[1] > t ? t + Link.CR : t - Link.CR, ys[1]);
        context.lineTo(xs[1], ys[1]);
        context.stroke();
        break;
      case Link.AUTOLINK_HV:
        context.beginPath();
        context.moveTo(xs[0], ys[0]);
        context.lineTo(xs[1] > xs[0] ? xs[1] -Link.CR : xs[1] + Link.CR, ys[0]);
        context.quadraticCurveTo(xs[1], ys[0], xs[1], ys[1] > ys[0] ? ys[0] + Link.CR : ys[0] - Link.CR);
        context.lineTo(xs[1], ys[1]);
        context.stroke();
        break;
      case Link.AUTOLINK_VH:
        context.beginPath();
        context.moveTo(xs[0], ys[0]);
        context.lineTo(xs[0], ys[1] > ys[0] ? ys[1] - Link.CR : ys[1] + Link.CR);
        context.quadraticCurveTo(xs[0], ys[1], xs[1] > xs[0] ? xs[0] + Link.CR : xs[0] - Link.CR, ys[1]);
        context.lineTo(xs[1], ys[1]);
        context.stroke();
        break;
    }
  };
  
  Link.prototype.getAutoElbowLinkType = function() {
    var type = this.display.type;
    var xs = this.display.xs;
    var ys = this.display.ys;
    
    if (type == Link.ELBOWH) {
      if (xs[0] == xs[1]) {
        return Link.AUTOLINK_V; 
      } 
      else if (ys[0] == ys[1]) {
        return Link.AUTOLINK_H;
      } 
      else if (Math.abs(this.to.display.x - this.from.display.x) > Link.ELBOW_VH_THRESHOLD) {
        return Link.AUTOLINK_HVH;
      }
      else {
        return Link.AUTOLINK_HV;
      }
    } 
    else if (type === Link.ELBOWV) {
      if (xs[0] == xs[1]) {
        return Link.AUTOLINK_V; 
      }
      else if (ys[0] == ys[1]) {
        return Link.AUTOLINK_H;
      }
      else if (Math.abs(this.to.display.y - this.from.display.y) > Link.ELBOW_VH_THRESHOLD) {
        return Link.AUTOLINK_VHV;
      }
      else {
        return Link.AUTOLINK_VH;
      }
    } 
    else {
      if (xs[0] == xs[1]) {
        return Link.AUTOLINK_V; 
      }
      else if (ys[0] == ys[1]) {
        return Link.AUTOLINK_H;
      } 
      else if (Math.abs(this.to.display.x - this.from.display.x) < Math.abs(this.to.display.y - this.from.display.y) * Link.ELBOW_THRESHOLD) {
        return Link.AUTOLINK_VHV;
      } 
      else if (Math.abs(this.to.display.y - this.from.display.y) < Math.abs(this.to.display.x - this.from.display.x) * Link.ELBOW_THRESHOLD) {
        return Link.AUTOLINK_HVH;
      } 
      else {
        return Link.AUTOLINK_HV;
      }
    }
  };
  
  
  Link.prototype.getEventColor = function(eventName) {
    if (eventName === 'START' || eventName === 'RESUME')
      return 'green';
    else if (eventName === 'DELAY' || eventName === 'HOLD')
      return 'orange';
    else if (eventName === 'ERROR' || eventName === 'ABORT')
      return 'red';
    else
      return 'gray';
  };
  
  return Link;
    
}]);
