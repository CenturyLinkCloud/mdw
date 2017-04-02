'use strict';

var dcMod = angular.module('drawingConstants', []);

dcMod.constant('DC', {
  DEFAULT_FONT: {
    FONT: '12px sans-serif',
    SIZE: 12,
  },
  TITLE_FONT: {
    FONT: 'bold 18px sans-serif',
    SIZE: 18
  },
  DEFAULT_COLOR: 'black',
  HYPERLINK_COLOR: '#1565c0',
  META_COLOR: 'gray',
  BOX_OUTLINE_COLOR: 'black',
  BOX_ROUNDING_RADIUS: 12,
  ANCHOR_W: 2,
  ANCHOR_HIT_W: 7
  
});
