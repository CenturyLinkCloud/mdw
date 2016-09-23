// Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
'use strict';

var linkMod = angular.module('mdwLink', ['mdw']);

linkMod.factory('Link', ['mdw', 'util', function(mdw, util) {

  var Link = function(transition, from, to) {
    this.transition = transition;
    this.from = from;
    this.to = to;
    this.workflowType = 'transition';
  };
  
  Link.DEFAULT_COLOR = 'black';
  Link.DEFAULT_FONT_SIZE = 12;
  Link.GAP = 4;
  Link.CR = 8;
  Link.LINK_WIDTH = 3;
  Link.CORR = 3; // offset for link start points (TODO: why?)

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
    diagram.context.fillStyle = this.getEventColor(this.transition.event);
    this.drawConnector(diagram.context);
    // todo draw shape

    // title
    diagram.context.fillText(this.title.text, this.title.x, this.title.y);
    diagram.context.strokeStyle = Link.DEFAULT_COLOR;
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
        // TODO: make use of Link.CORR
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
                context.quadraticCurveTo(xs[i], ys[i], xs[i+1] > xs[i] ? xs[i] + Link.CR : xs[i] - Link.CR, ys[i]);
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
    
    this.drawConnectorArrow(context);
  };
  
  Link.prototype.drawAutoElbowConnector = function(context) {
    var xs = this.display.xs;
    var ys = this.display.ys;
    var t;
    var xcorr = xs[0] < xs[1] ? Link.CORR : -Link.CORR;
    var ycorr = ys[0] < ys[1] ? Link.CORR : -Link.CORR;
    switch (this.getAutoElbowLinkType()) {
      case Link.AUTO_ELBOW_LINK_TYPES.AUTOLINK_H:
        context.beginPath();
        context.moveTo(xs[0] - xcorr, ys[0]);
        context.lineTo(xs[1], ys[1]);
        context.stroke();
        break;
      case Link.AUTO_ELBOW_LINK_TYPES.AUTOLINK_V:
        context.beginPath();
        context.moveTo(xs[0], ys[0] - ycorr);
        context.lineTo(xs[1], ys[1]);
        context.stroke();
        break;
      case Link.AUTO_ELBOW_LINK_TYPES.AUTOLINK_HVH:
        context.beginPath();
        t = (xs[0] + xs[1]) / 2;
        context.moveTo(xs[0] - xcorr, ys[0]);
        context.lineTo(t > xs[0] ? t - Link.CR : t + Link.CR, ys[0]);
        context.quadraticCurveTo(t, ys[0], t, ys[1] > ys[0] ? ys[0] + Link.CR : ys[0] - Link.CR);
        context.lineTo(t, ys[1] > ys[0] ? ys[1] - Link.CR : ys[1] + Link.CR);
        context.quadraticCurveTo(t, ys[1], xs[1] > t ? t + Link.CR : t - Link.CR, ys[1]);
        context.lineTo(xs[1], ys[1]);
        context.stroke();
        break;
      case Link.AUTO_ELBOW_LINK_TYPES.AUTOLINK_VHV:
        context.beginPath();
        t = (ys[0] + ys[1]) / 2;
        context.moveTo(xs[0], ys[0] - ycorr);
        context.lineTo(xs[0], t > ys[0] ? t - Link.CR : t + Link.CR);
        context.quadraticCurveTo(xs[0], t, xs[1] > xs[0] ? xs[0] + Link.CR : xs[0] - Link.CR, t);
        context.lineTo(xs[1] > xs[0] ? xs[1] - Link.CR : xs[1] + Link.CR, t);
        context.quadraticCurveTo(xs[1], t, xs[1], ys[1] > t ? t + Link.CR : t-Link.CR);
        context.lineTo(xs[1], ys[1]);
        context.stroke();
        break;
      case Link.AUTO_ELBOW_LINK_TYPES.AUTOLINK_HV:
        context.beginPath();
        context.moveTo(xs[0] - xcorr, ys[0]);
        context.lineTo(xs[1] > xs[0] ? xs[1] -Link.CR : xs[1] + Link.CR, ys[0]);
        context.quadraticCurveTo(xs[1], ys[0], xs[1], ys[1] > ys[0] ? ys[0] + Link.CR : ys[0] - Link.CR);
        context.lineTo(xs[1], ys[1]);
        context.stroke();
        break;
      case Link.AUTO_ELBOW_LINK_TYPES.AUTOLINK_VH:
        context.beginPath();
        context.moveTo(xs[0], ys[0] - ycorr);
        context.lineTo(xs[0], ys[1] > ys[0] ? ys[1] - Link.CR : ys[1] + Link.CR);
        context.quadraticCurveTo(xs[0], ys[1], xs[1] > xs[0] ? xs[0] + Link.CR : xs[0] - Link.CR, ys[1]);
        context.lineTo(xs[1], ys[1]);
        context.stroke();
        break;
    }
  };
  
  Link.prototype.drawConnectorArrow = function(context) {
    var type = this.display.type;
    var xs = this.display.xs;
    var ys = this.display.ys;
    var p = 12;
    var slope;
    var x, y;
    // TODO from Link.java determineArrow() method (only ARROW_STYLE_END)
    if (type == Link.LINK_TYPES.CURVE) {
      if (xs.length == 4) {
        
      }
      else if (xs.length == 3) {

      } 
      else if (this.from !== this.to) {
      
      } 
      else {
      
      }
    }
    else if (type == Link.LINK_TYPES.STRAIGHT) {
      var p2 = xs.length - 1;
      var p1 = p2 - 1;
      x = xs[p2];
      y = ys[p2];
      slope = this.getSlope(xs[p1], ys[p1], xs[p2], ys[p2]);
    } 
    else if (xs.length == 2) {  // auto ELBOW/ELBOWH/ELBOWV type
      switch (this.getAutoElbowLinkType()) {
        case Link.AUTO_ELBOW_LINK_TYPES.AUTOLINK_V:
        case Link.AUTO_ELBOW_LINK_TYPES.AUTOLINK_VHV:                   
        case Link.AUTO_ELBOW_LINK_TYPES.AUTOLINK_HV:
          x = xs[1];
          y = ys[1] > ys[0] ? ys[1] + Link.GAP : ys[1] - Link.GAP;
          slope = ys[1] > ys[0] ? Math.PI/2 : Math.PI*1.5;
          break;
        case Link.AUTO_ELBOW_LINK_TYPES.AUTOLINK_H:
        case Link.AUTO_ELBOW_LINK_TYPES.AUTOLINK_HVH:
        case Link.AUTO_ELBOW_LINK_TYPES.AUTOLINK_VH:
          x = xs[1] > xs[0] ? xs[1] + Link.GAP : xs[1] - Link.GAP;
          y = ys[1];
          slope = xs[1] > xs[0] ? 0 : Math.PI;
          break;
      }                
    } 
    else {  // ELBOW/ELBOWH/ELBOWV, control points > 2
      var k = xs.length - 1;
      if (xs[k] == xs[k-1] && (ys[k] != ys[k-1] || ys[k-1] == ys[k-2])) {
        // verticle arrow
        x = xs[k];
        y = ys[k] > ys[k-1] ? ys[k] + Link.GAP : ys[k] - Link.GAP;
        slope = ys[k] > ys[k-1] ? Math.PI/2 : Math.PI*1.5;              
      } 
      else {
        x = xs[k] > xs[k-1] ? xs[k] + Link.GAP : xs[k] - Link.GAP;
        y = ys[k];
        slope = xs[k] > xs[k-1] ? 0 : Math.PI;
      }
    }
    // convert point and slope to polygon
    var dl = slope - 2.7052;  // 25 degrees
    var dr = slope + 2.7052;  // 25 degrees
    
    context.beginPath();
    context.moveTo(x, y);
    context.lineTo(Math.round(Math.cos(dl)*p + x), Math.round(Math.sin(dl)*p + y));
    context.lineTo(Math.round(Math.cos(dr)*p + x), Math.round(Math.sin(dr)*p + y));
    context.fill();
    context.stroke();
  };
  
  Link.prototype.getAutoElbowLinkType = function() {
    var type = this.display.type;
    var xs = this.display.xs;
    var ys = this.display.ys;
    
    if (type == Link.ELBOWH) {
      if (xs[0] == xs[1]) {
        return Link.AUTO_ELBOW_LINK_TYPES.AUTOLINK_V; 
      } 
      else if (ys[0] == ys[1]) {
        return Link.AUTO_ELBOW_LINK_TYPES.AUTOLINK_H;
      } 
      else if (Math.abs(this.to.display.x - this.from.display.x) > Link.ELBOW_VH_THRESHOLD) {
        return Link.AUTO_ELBOW_LINK_TYPES.AUTOLINK_HVH;
      }
      else {
        return Link.AUTO_ELBOW_LINK_TYPES.AUTOLINK_HV;
      }
    } 
    else if (type === Link.ELBOWV) {
      if (xs[0] == xs[1]) {
        return Link.AUTO_ELBOW_LINK_TYPES.AUTOLINK_V; 
      }
      else if (ys[0] == ys[1]) {
        return Link.AUTO_ELBOW_LINK_TYPES.AUTOLINK_H;
      }
      else if (Math.abs(this.to.display.y - this.from.display.y) > Link.ELBOW_VH_THRESHOLD) {
        return Link.AUTO_ELBOW_LINK_TYPES.AUTOLINK_VHV;
      }
      else {
        return Link.AUTO_ELBOW_LINK_TYPES.AUTOLINK_VH;
      }
    } 
    else {
      if (xs[0] == xs[1]) {
        return Link.AUTO_ELBOW_LINK_TYPES.AUTOLINK_V; 
      }
      else if (ys[0] == ys[1]) {
        return Link.AUTO_ELBOW_LINK_TYPES.AUTOLINK_H;
      } 
      else if (Math.abs(this.to.display.x - this.from.display.x) < Math.abs(this.to.display.y - this.from.display.y) * Link.ELBOW_THRESHOLD) {
        return Link.AUTO_ELBOW_LINK_TYPES.AUTOLINK_VHV;
      } 
      else if (Math.abs(this.to.display.y - this.from.display.y) < Math.abs(this.to.display.x - this.from.display.x) * Link.ELBOW_THRESHOLD) {
        return Link.AUTO_ELBOW_LINK_TYPES.AUTOLINK_HVH;
      } 
      else {
        return Link.AUTO_ELBOW_LINK_TYPES.AUTOLINK_HV;
      }
    }
  };
  
  // in polar degrees
  Link.prototype.getSlope = function(x1, y1, x2, y2) {
    var slope;
    if (x1 == x2) 
      slope = (y1 < y2) ? Math.PI / 2 : -Math.PI / 2;
    else
      slope = Math.atan(y2 - y1)/(x2 - x1);
    if (x1 > x2) {
        if (slope > 0)
          slope -= Math.PI;
        else 
          slope += Math.PI;
    }
    return slope;
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
