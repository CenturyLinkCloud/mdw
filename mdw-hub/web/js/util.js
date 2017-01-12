// Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
'use strict';

var utilMod = angular.module('util', []);

utilMod.factory('util', function() {
  return {
    months: ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'],
    dayMs: 24 * 3600 * 1000, 
    monthAndDay: function(date) {
      return this.months[date.getMonth()] + ' ' + date.getDate();
    },
    serviceDate: function(date) {
      return date.getFullYear() + '-' + this.months[date.getMonth()] + '-' + this.padLeading(date.getDate(), 2, '0');
    },
    formatDateTime: function(date) {
      var ampm = 'am';
      var hours = date.getHours();
      if (hours > 11)
        ampm = 'pm';
      if (hours > 12)
        hours = hours - 12;
      return this.serviceDate(date) + ' ' + hours + ':' + this.padLeading(date.getMinutes(), 2, '0') + ':' + this.padLeading(date.getSeconds(), 2, '0') + ' ' + ampm;
    },
    correctDbDate: function(date, dbDate) {
      var adj = Date.now() - dbDate.getTime();
      return new Date(date.getTime() + adj);
    },
    past: function(date) {
      var ret = this.months[date.getMonth()] + ' ' + date.getDate();
      var agoMs = Date.now() - date.getTime();
      if (agoMs < 60000) {
        var secs = Math.round(agoMs/1000);
        ret =  secs + (secs == 1 ? ' second ago' : ' seconds ago');
      }
      else if (agoMs < 3600000) {
        var mins = Math.round(agoMs/60000);
        ret = mins + (mins == 1 ? ' minute ago' : ' minutes ago');
      }
      else if (agoMs < 86400000) {
        var hrs = Math.round(agoMs/3600000);
        ret = hrs + (hrs == 1 ? ' hour ago' : ' hours ago');
      }
      else if (agoMs < 2592000000) {
        var days = Math.round(agoMs/86400000);
        ret = days + (days == 1 ? ' day ago' : ' days ago');
      }
      return ret;
    },
    future: function(date) {
      var ret = this.months[date.getMonth()] + ' ' + date.getDate();
      var inMs = date.getTime() - Date.now();
      if (inMs < 60000) {
        var secs = Math.round(inMs/1000);
        ret =  'in ' + secs + (secs == 1 ? ' second' : ' seconds');
      }
      else if (inMs < 3600000) {
        var mins = Math.round(inMs/60000);
        ret = 'in ' + mins + (mins == 1 ? ' minute' : ' minutes');
      }
      else if (inMs < 86400000) {
        var hrs = Math.round(inMs/3600000);
        ret = 'in ' + hrs + (hrs == 1 ? ' hour' : ' hours');
      }
      else if (inMs < 2592000000) {
        var days = Math.round(inMs/86400000);
        ret = 'in ' + days + (days == 1 ? ' day' : ' days');
      }
      return ret;
    },
    padLeading: function(str, len, ch) {
      var ret = '' + str; // convert if nec.
      if (!ch)
        ch = ' ';
      while (ret.length < len)
        ret = ch + ret;
      return ret;
    },
    padTrailing: function(str, len, ch) {
      var ret = '' + str; // convert if nec.
      if (!ch)
        ch = ' ';
      while (ret.length < len)
        ret = ret + ch;
      return ret;
    },
    isMobile: function() {
      var check = false;
      // regex from detectmobilebrowsers.com
      (function(a){if(/(android|bb\d+|meego).+mobile|avantgo|bada\/|blackberry|blazer|compal|elaine|fennec|hiptop|iemobile|ip(hone|od)|iris|kindle|lge |maemo|midp|mmp|mobile.+firefox|netfront|opera m(ob|in)i|palm( os)?|phone|p(ixi|re)\/|plucker|pocket|psp|series(4|6)0|symbian|treo|up\.(browser|link)|vodafone|wap|windows ce|xda|xiino|android|ipad|playbook|silk/i.test(a)||/1207|6310|6590|3gso|4thp|50[1-6]i|770s|802s|a wa|abac|ac(er|oo|s\-)|ai(ko|rn)|al(av|ca|co)|amoi|an(ex|ny|yw)|aptu|ar(ch|go)|as(te|us)|attw|au(di|\-m|r |s )|avan|be(ck|ll|nq)|bi(lb|rd)|bl(ac|az)|br(e|v)w|bumb|bw\-(n|u)|c55\/|capi|ccwa|cdm\-|cell|chtm|cldc|cmd\-|co(mp|nd)|craw|da(it|ll|ng)|dbte|dc\-s|devi|dica|dmob|do(c|p)o|ds(12|\-d)|el(49|ai)|em(l2|ul)|er(ic|k0)|esl8|ez([4-7]0|os|wa|ze)|fetc|fly(\-|_)|g1 u|g560|gene|gf\-5|g\-mo|go(\.w|od)|gr(ad|un)|haie|hcit|hd\-(m|p|t)|hei\-|hi(pt|ta)|hp( i|ip)|hs\-c|ht(c(\-| |_|a|g|p|s|t)|tp)|hu(aw|tc)|i\-(20|go|ma)|i230|iac( |\-|\/)|ibro|idea|ig01|ikom|im1k|inno|ipaq|iris|ja(t|v)a|jbro|jemu|jigs|kddi|keji|kgt( |\/)|klon|kpt |kwc\-|kyo(c|k)|le(no|xi)|lg( g|\/(k|l|u)|50|54|\-[a-w])|libw|lynx|m1\-w|m3ga|m50\/|ma(te|ui|xo)|mc(01|21|ca)|m\-cr|me(rc|ri)|mi(o8|oa|ts)|mmef|mo(01|02|bi|de|do|t(\-| |o|v)|zz)|mt(50|p1|v )|mwbp|mywa|n10[0-2]|n20[2-3]|n30(0|2)|n50(0|2|5)|n7(0(0|1)|10)|ne((c|m)\-|on|tf|wf|wg|wt)|nok(6|i)|nzph|o2im|op(ti|wv)|oran|owg1|p800|pan(a|d|t)|pdxg|pg(13|\-([1-8]|c))|phil|pire|pl(ay|uc)|pn\-2|po(ck|rt|se)|prox|psio|pt\-g|qa\-a|qc(07|12|21|32|60|\-[2-7]|i\-)|qtek|r380|r600|raks|rim9|ro(ve|zo)|s55\/|sa(ge|ma|mm|ms|ny|va)|sc(01|h\-|oo|p\-)|sdk\/|se(c(\-|0|1)|47|mc|nd|ri)|sgh\-|shar|sie(\-|m)|sk\-0|sl(45|id)|sm(al|ar|b3|it|t5)|so(ft|ny)|sp(01|h\-|v\-|v )|sy(01|mb)|t2(18|50)|t6(00|10|18)|ta(gt|lk)|tcl\-|tdg\-|tel(i|m)|tim\-|t\-mo|to(pl|sh)|ts(70|m\-|m3|m5)|tx\-9|up(\.b|g1|si)|utst|v400|v750|veri|vi(rg|te)|vk(40|5[0-3]|\-v)|vm40|voda|vulc|vx(52|53|60|61|70|80|81|83|85|98)|w3c(\-| )|webc|whit|wi(g |nc|nw)|wmlb|wonu|x700|yas\-|your|zeto|zte\-/i.test(a.substr(0,4)))check = true})(navigator.userAgent||navigator.vendor||window.opera); // jshint ignore:line
      return check;
    },
    urlParams: function() {
      // params need to be on url before hash (eg: http://localhost:8080/mdw-admin/?mdwMobile=true#/tasks)
      var params = {};
      var search = /([^&=]+)=?([^&]*)/g;
      var match;
      while ((match = search.exec(window.location.search.substring(1))) !== null)
       params[decodeURIComponent(match[1])] = decodeURIComponent(match[2]);
      return params;
    },
    getProperties: function(obj) {
      var props = [];
      for (var prop in obj) {
        if (obj.hasOwnProperty(prop)) {
          props.push(prop);
        }
      }
      return props;
    },
    isEmpty: function(obj) {
      return !obj || (Object.keys(obj).length === 0 && obj.constructor === Object);
    }
  };
});