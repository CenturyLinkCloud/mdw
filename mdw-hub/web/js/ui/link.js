'use strict';

var linkMod = angular.module('mdwLink', ['mdw']);

linkMod.factory('Link', ['mdw', 'util', 'DC',
                         function(mdw, util, DC) {

  var Link = function(transition, from, to) {
    this.transition = transition;
    this.from = from;
    this.to = to;
    this.workflowType = 'transition';
  };
  
  Link.INITIATED = 'blue';
  Link.TRAVERSED = 'black';
  Link.UNTRAVERSED = '#9e9e9e';
  Link.GAP = 4;
  Link.CR = 8;
  Link.LINK_WIDTH = 3;
  Link.CORR = 3; // offset for link start points (TODO: why?)

  Link.EVENTS = {
    START: {color: 'green'},
    RESUME: {color: 'green'},
    DELAY: {color: 'orange'},
    HOLD: {color: 'orange'},
    ERROR: {color: 'red'},
    ABORT: {color: 'red'},
    CORRECT: {color: 'purple'},
    FINISH: {color: 'gray'}
  };
  
  Link.LINK_TYPES = {
    STRAIGHT: 'Straight',
    ELBOW: 'Elbow',
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
    var color = Link.EVENTS[this.transition.event].color;
    if (diagram.instance) {
      if (this.instances && this.instances.length > 0) {
        var latest = this.instances[0];
        if (latest.statusCode == 1)
          color = Link.INITIATED;
        else
          color = Link.TRAVERSED;
      }
      else {
        color = Link.UNTRAVERSED;
      }
    }
      
    diagram.context.strokeStyle = color;
    diagram.context.fillStyle = color;
    this.drawConnector(diagram.context);
    // todo draw shape

    // title
    if (diagram.instance && (!this.instances || this.instances.length === 0))
      diagram.context.fillStyle = Link.UNTRAVERSED;
    diagram.context.fillText(this.title.text, this.title.x, this.title.y);
    diagram.context.strokeStyle = DC.DEFAULT_COLOR;
    diagram.context.fillStyle = DC.DEFAULT_COLOR;
    
  };
  
  // sets display/title and returns an object with w and h for required size
  Link.prototype.prepareDisplay = function(diagram) {
    var maxDisplay = { w: 0, h: 0};
    this.display = this.getDisplay();
    // TODO determine effect on maxDisplay
    
    // title
    var label = this.transition.event === 'FINISH' ? '' : this.transition.event + ':';
    label += this.transition.resultCode ? this.transition.resultCode : ''; 
    this.title = { text: label };
    // TODO title coords
    this.title.x = this.display.lx;
    this.title.y = this.display.ly + DC.DEFAULT_FONT_SIZE;
    
    return maxDisplay;
  };
  
  Link.prototype.getDisplay = function() {
    var display = {};
    var displayAttr = this.transition.attributes.TRANSITION_DISPLAY_INFO;
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
  
  Link.prototype.setDisplay = function(display) {
    if (!this.transition.attributes)
      this.transition.attributes = {};
    var attr = 'type=' + display.type + ',lx=' + display.lx + ',ly=' + display.ly;
    attr += ',xs=';
    for (var i = 0; i < display.xs.length; i++) {
      if (i > 0)
        attr += '&';
      attr += display.xs[i];
    }
    attr += ',ys=';
    for (var i = 0; i < display.ys.length; i++) {
      if (i > 0)
        attr += '&';
      attr += display.ys[i];
    }
    this.transition.attributes.TRANSITION_DISPLAY_INFO = attr;
  };
  
  Link.prototype.applyState = function(transitionInstances) {
    this.instances = transitionInstances;
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
    if (type == Link.LINK_TYPES.STRAIGHT) {
      var p2 = xs.length - 1;
      var p1 = p2 - 1;
      x = xs[p2];
      y = ys[p2];
      slope = this.getSlope(xs[p1], ys[p1], xs[p2], ys[p2]);
    } 
    else if (xs.length == 2) {  
      // auto ELBOW/ELBOWH/ELBOWV type
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
    else {  
      // ELBOW/ELBOWH/ELBOWV, control points > 2
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

  Link.prototype.recalc = function(step) {
    var type = this.display.type;
    var xs = this.display.xs;
    var ys = this.display.ys;
    var lx = this.display.lx;
    var ly = this.display.ly;
  
    var n = xs.length;
    // remember relative label position
    var labelSlope = this.getSlope(xs[0], ys[0], lx, ly) - this.getSlope(xs[0], ys[0], xs[n-1], ys[n-1]);
    var labelDist = this.getDist(xs[0], ys[0], xs[n-1], ys[n-1]);
    if (labelDist < 5.0) 
      labelDist = 0.5;
    else 
      labelDist = this.getDist(xs[0], ys[0], lx, ly) / labelDist;
    
    if (type == Link.LINK_TYPES.STRAIGHT) {
      if (n == 2) {
        this.calc();
      }
      else {
        if (step == from) {
          if (xs[1] > step.display.x + step.display.w + Link.GAP) 
            xs[0] = step.display.x + step.display.w + Link.GAP;
          else if (xs[1] < step.display.x - Link.GAP)
            xs[0] = step.display.x - Link.GAP;
          if (ys[1] > step.display.y + step.display.h + Link.GAP) 
            ys[0] = step.display.y + step.display.h + Link.GAP;
          else if (ys[1] < step.display.y - Link.GAP)
            ys[0] = step.display.y - Link.GAP;
        } 
        else {
          var k = n - 1;
          if (xs[k-1] > step.display.x + step.display.w + Link.GAP)
            xs[k] = step.display.x + step.display.w + Link.GAP;
          else if (xs[k-1] < step.display.x - Link.GAP)
            xs[k] = step.display.x - Link.GAP;
          if (ys[k-1] > step.display.y + step.display.h + Link.GAP) 
            ys[k] = step.display.y + step.display.h + Link.GAP;
          else if (ys[k-1] < step.display.y - Link.GAP)
            ys[k] = step.display.y - Link.GAP;
        }
      }
    } 
    else if (n == 2) {
      // automatic ELBOW, ELBOWH, ELBOWV
      this.calcAutoElbow();
    } 
    else {
      // controlled ELBOW, ELBOWH, ELBOWV
      var wasHorizontal = !this.isAnchorHorizontal(0);
      var horizontalFirst = (Math.abs(this.from.display.x - this.to.display.x) >= Math.abs(this.from.display.y - this.to.display.y));
      if (type == Link.LINK_TYPES.ELBOW && wasHorizontal != horizontalFirst) {
        this.calc();
      } 
      else if (step == this.from) {
        if (xs[1] > step.display.x + step.display.w)
          xs[0] = step.display.x + step.display.w + Link.GAP;
        else if (xs[1] < step.display.x) 
          xs[0] = step.display.x - Link.GAP;
        else 
          xs[0] = xs[1];
            
        if (ys[1] > step.display.y + step.display.h) 
          ys[0] = step.display.y + step.display.h + Link.GAP;
        else if (ys[1] < step.display.y) 
          ys[0] = step.display.y - Link.GAP;
        else ys[0] = ys[1];
            
        if (wasHorizontal) 
          ys[1] = ys[0];
        else 
          xs[1] = xs[0];
      }
      else {
        var k = n - 1;
        if (xs[k-1] > step.display.x + step.display.w)
          xs[k] = step.display.x + step.display.w + Link.GAP;
        else if (xs[k-1] < step.display.x) 
          xs[k] = step.display.x - Link.GAP;
        else 
          xs[k] = xs[k-1];
        
        if (ys[k-1] > step.display.y + step.display.h) 
          ys[k] = step.display.y + step.display.h + Link.GAP;
        else if (ys[k-1] < step.display.y) 
          ys[k] = step.display.y - Link.GAP;
        else 
          ys[k] = ys[k-1];
            
        if ((wasHorizontal && n % 2 == 0) || (!wasHorizontal && n % 2 != 0))
          ys[k-1] = ys[k];
        else 
          xs[k-1] = xs[k];
      }
    }
    
    // label position
    labelSlope = this.getSlope(xs[0], ys[0], xs[n-1], ys[n-1]) + labelSlope;
    labelDist = this.getDist(xs[0], ys[0], xs[n-1], ys[n-1]) * labelDist;
    this.display.lx = Math.round(xs[0] + Math.cos(labelSlope) * labelDist);
    this.display.ly = Math.round(ys[0] + Math.sin(labelSlope) * labelDist);

    this.setDisplay(this.display);
  };

  Link.prototype.calc = function() {
    var type = this.display.type;
    var xs = this.display.xs;
    var ys = this.display.ys;
    var x1 = this.from.display.x;
    var y1 = this.from.display.y;
    var w1 = this.from.display.w;
    var h1 = this.from.display.h;
    var x2 = this.to.display.x;
    var y2 = this.to.display.y;
    var w2 = this.to.display.w;
    var h2 = this.to.display.h;
    
    var n = xs.length < 2 ? 2 : xs.length;
    
    if (type == Link.LINK_TYPES.STRAIGHT) {
      xs = this.display.xs = [];
      ys = this.display.ys = [];
      for (var a = 0; a < n; a++) {
        xs.push(0);
        ys.push(0);
      }
        
      if (Math.abs(x1 - x2) >= Math.abs(y1 - y2)) {
        // more of a horizontal link
        xs[0] = (x1 <= x2) ? (x1 + w1) : x1;
        ys[0] = y1 + h1 / 2;
        xs[n-1] = (x1 <= x2) ? x2 : (x2 + w2);
        ys[n-1] = y2 + h2 / 2;
        for (var i = 1; i < n - 1; i++) {
          if (i % 2 != 0) {
            ys[i] = ys[i-1];
            xs[i] = (xs[n-1] -xs[0]) * ((i + 1) / 2) / (n / 2) + xs[0];
          } 
          else {
            xs[i] = xs[i-1];
            ys[i] = (ys[n-1] - ys[0]) * ((i + 1) / 2) / ((n - 1) / 2) + ys[0];
          }
        }
      }
      else {
        // more of a vertical link
        xs[0] = x1 + w1 / 2;
        ys[0] = (y1 <= y2) ? (y1 + h1) : y1;
        xs[n-1] = x2 + w2 / 2;
        ys[n-1] = (y1 <= y2) ? y2 : (y2 + h2);
        for (var i = 1; i < n - 1; i++) {
          if (i % 2 != 0) {
            xs[i] = xs[i-1];
            ys[i] = (ys[n-1] - ys[0]) * ((i + 1) / 2) / (n / 2) + ys[0];
          } 
          else {
            ys[i] = ys[i-1];
            xs[i] = (xs[n-1] - xs[0]) * (i / 2) / ((n - 1) / 2) + xs[0];
          }
        }
      }
    }
    else if (n == 2) {
      // auto ELBOW, ELBOWH, ELBOWV 
      xs = this.display.xs = [0, 0];
      ys = this.display.xs = [0, 0];
      this.calcAutoElbow();
    } 
    else {
      // ELBOW, ELBOWH, ELBOWV with middle control points
      var horizontalFirst = type == Link.LINK_TYPES.ELBOWH || (type == Link.LINK_TYPES.ELBOWH && Math.abs(x1 - x2) >= Math.abs(y1 - y2));
      var evenN = n % 2 == 0;
      var horizontalLast = (horizontalFirst && evenN) || (!horizontalFirst && !evenN);
      xs = this.display.xs = [];
      ys = this.display.ys = [];
      for (a = 0; a < n; a++) {
        xs.push(0);
        ys.push(0);
      }
      if (horizontalFirst) {
        xs[0] = (x1 <= x2) ? (x1 + w1) : x1;
        ys[0] = y1 + h1 / 2;
      } 
      else {
        xs[0] = x1 + w1 / 2;
        ys[0] = (y1 <= y2) ? (y1 + h1) : y1;
      }
      if (horizontalLast) {
        xs[n-1] = (x2 <= x1) ? (x2 + w2) : x2;
        ys[n-1] = y2 + h2 / 2;
      } 
      else {
        xs[n-1] = x2 + w2 / 2;
        ys[n-1] = (y2 <= y1) ? (y2 + h2) : y2;
      }
      if (horizontalFirst) {
        for (var i = 1; i < n - 1; i++) {
          if (i % 2 != 0) {
            ys[i] = ys[i-1];
            xs[i] = (xs[n-1] - xs[0]) * ((i + 1) / 2) / (n / 2) + xs[0];
          } 
          else {
            xs[i] = xs[i-1];
            ys[i] = (ys[n-1] - ys[0]) * ((i + 1) / 2) / ((n - 1) / 2) + ys[0];
          }
        }
      } 
      else {
        for (var i = 1; i < n - 1; i++) {
          if (i % 2 != 0) {
            xs[i] = xs[i-1];
            ys[i] = (ys[n-1] - ys[0]) * ((i + 1) / 2) / (n / 2) + ys[0];
          } 
          else {
            ys[i] = ys[i-1];
            xs[i] = (xs[n-1] - xs[0]) * (i / 2) / ((n - 1) / 2) + xs[0];
          }
        }
      }
    }
    this.calcLabel();
    this.setDisplay(this.display);
  };
  
  Link.prototype.isAnchorHorizontal = function(anchor) {
    var p = anchor - 1;
    var n = anchor + 1;
    if (p >= 0 && this.display.xs[p] != this.display.xs[anchor] && this.display.ys[p] == this.display.ys[anchor]) {
      return true;
    } 
    else if (n < this.display.xs.length && this.display.xs[n] == this.display.xs[anchor] && this.display.ys[n] != this.display.ys[anchor]) {
      return true;
    } 
    else {
      return false;
    }
  };
  
  Link.prototype.calcLabel = function() {
    var type = this.display.type;
    var xs = this.display.xs;
    var ys = this.display.ys;
  
    var x1 = this.from.display.x;
    var y1 = this.from.display.y;
    var x2 = this.to.display.x;
    
    var n = xs.length;

    if (type == Link.LINK_TYPES.STRAIGHT) {
      this.display.lx = (xs[0] + xs[n-1]) / 2;
      this.display.ly = (ys[0] + ys[n-1]) / 2;
    } 
    else if (n == 2) {
      // auto ELBOW, ELBOWH, ELBOWV 
      this.display.lx = (xs[0] + xs[n-1]) / 2;
      this.display.ly = (ys[0] + ys[n-1]) / 2;
    } 
    else {    
      // ELBOW, ELBOWH, ELBOWV with middle control points
      var horizontalFirst = ys[0] == ys[1];
      if (n <= 3) {
        if (horizontalFirst) {
          this.display.lx = (x1 + x2) / 2 - 40;
          this.display.ly = ys[0] - 4;
        } 
        else {
          this.display.lx = xs[0] + 2;
          this.display.ly = (ys[0] + ys[1]) / 2;
        }
      } 
      else {
        if (horizontalFirst) {
          this.display.lx = (x1 <= x2) ? (xs[(n-1) / 2] + 2) : (xs[(n-1)/2 + 1] + 2);
          this.display.ly = ys[n/2] - 4;
        } 
        else {
          this.display.lx = (x1 <= x2) ? xs[n/2 - 1] : xs[n/2];
          this.display.ly = ys[n/2 - 1] - 4;
        }
      }
    }
  };
  
  Link.prototype.calcAutoElbow = function() {
    var type = this.display.type;
    var xs = this.display.xs;
    var ys = this.display.ys;
    
    if (this.to.display.x + this.to.display.w >= this.from.display.x && this.to.display.x <= this.from.display.x + this.from.display.w) {
      // V
      xs[0] = xs[1] = (Math.max(this.from.display.x, this.to.display.x) + Math.min(this.from.display.x + this.from.display.w, this.to.display.x + this.to.display.w)) / 2;
      if (this.to.display.y > this.from.display.y) {
        ys[0] = this.from.display.y + this.from.display.h + Link.GAP;
        ys[1] = this.to.display.y - Link.GAP;
      } 
      else {
        ys[0] = this.from.display.y - Link.GAP;
        ys[1] = this.to.display.y + this.to.display.h + Link.GAP;
      }
    } 
    else if (this.to.display.y + this.to.display.h>= this.from.display.y && this.to.display.y <= this.from.display.y + this.from.display.h) {
      // H
      ys[0] = ys[1] = (Math.max(this.from.display.y, this.to.display.y) + Math.min(this.from.display.y + this.from.display.h, this.to.display.y + this.to.display.h)) / 2;
      if (this.to.display.x > this.from.display.x) {
          xs[0] = this.from.display.x + this.from.display.w + Link.GAP;
          xs[1] = this.to.display.x - Link.GAP;
      }
      else {
          xs[0] = this.from.display.x - Link.GAP;
          xs[1] = this.to.display.x + this.to.display.w + Link.GAP;
      }
    }
    else if ((type == Link.LINK_TYPES.ELBOW && Math.abs(this.to.display.x - this.from.display.x) < Math.abs(this.to.display.y - this.from.display.y) * Link.ELBOW_THRESHOLD)
          || (type == Link.LINK_TYPES.ELBOWV && Math.abs(this.to.display.y - this.from.display.y) > Link.ELBOW_VH_THRESHOLD)) {
      // VHV
      xs[0] = this.from.display.x + this.from.display.w / 2;
      xs[1] = this.to.display.x + this.to.display.w / 2;
      if (this.to.display.y > this.from.display.y) {
        ys[0] = this.from.display.y + this.from.display.h + Link.GAP;
        ys[1] = this.to.display.y - Link.GAP;
      } 
      else {
        ys[0] = this.from.display.y - Link.GAP;
        ys[1] = this.to.display.y + this.to.display.h + Link.GAP;
      }
    } 
    else if ((type == Link.LINK_TYPES.ELBOW && Math.abs(this.to.display.y - this.from.display.y) < Math.abs(this.to.display.x - this.from.display.x) * Link.ELBOW_THRESHOLD)
          || (type == Link.LINK_TYPES.ELBOWH && Math.abs(this.to.display.x - this.from.display.x) > Link.ELBOW_VH_THRESHOLD)) {
      // HVH
      ys[0] = this.from.display.y + this.from.display.h / 2;
      ys[1] = this.to.display.y + this.to.display.h / 2;
      if (this.to.display.x > this.from.display.x) {
        xs[0] = this.from.display.x + this.from.display.w + Link.GAP;
        xs[1] = this.to.display.x - Link.GAP;
      } 
      else {
        xs[0] = this.from.display.x - Link.GAP;
        xs[1] = this.to.display.x + this.to.display.w + Link.GAP;
      }
    } 
    else if (type == Link.LINK_TYPES.ELBOWV) {
      // VH
      if (this.to.display.y > this.from.display.y)
        ys[0] = this.from.display.y + this.from.display.h + Link.GAP;
      else 
        ys[0] = this.from.display.y - Link.GAP;
      xs[0] = this.from.display.x + this.from.display.w / 2;
      ys[1] = this.to.display.y + this.to.display.h / 2;
      if (this.to.display.x > this.from.display.x) 
        xs[1] = this.to.display.x - Link.GAP;
      else 
        xs[1] = this.to.display.x + this.to.display.w + Link.GAP;
    } 
    else {
      // HV
      if (this.to.display.x > this.from.display.x)
        xs[0] = this.from.display.x + this.from.display.w + Link.GAP;
      else 
        xs[0] = this.from.display.x - Link.GAP;
      ys[0] = this.from.display.y + this.from.display.h / 2;
      xs[1] = this.to.display.x + this.to.display.w / 2;
      if (this.to.display.y > this.from.display.y)
        ys[1] = this.to.display.y - Link.GAP;
      else 
        ys[1] = this.to.display.y + this.to.display.h + Link.GAP;
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
  
  Link.prototype.getDist = function(x1, y1, x2, y2) {
    return Math.sqrt((y2 - y1) * (y2 - y1) + (x2 - x1) * (x2 - x1));
  }
  
  return Link;
}]);
