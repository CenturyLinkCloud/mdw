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
  DEFAULT_LINE_WIDTH: 1,
  DEFAULT_COLOR: 'black',
  HYPERLINK_COLOR: '#1565c0',
  LINE_COLOR: 'green',
  META_COLOR: 'gray',
  BOX_OUTLINE_COLOR: 'black',
  TRANSPARENT: 'rgba(0, 0, 0, 0)',
  BOX_ROUNDING_RADIUS: 12,
  ANCHOR_W: 3,
  ANCHOR_COLOR: '#ec407a',
  ANCHOR_HIT_W: 8,
  MIN_DRAG: 3
  
});
