'use strict';

var dcMod = angular.module('drawingConstants', []);

dcMod.constant('DC', {
  DEFAULT_FONT_SIZE: 12,
  DEFAULT_FONT: '12px sans-serif',
  TITLE_FONT_SIZE: 18,
  TITLE_FONT: 'bold 18px sans-serif',
  DEFAULT_COLOR: 'black',
  HYPERLINK_COLOR: '#1565c0',
  META_COLOR: 'gray',
  BOX_OUTLINE_COLOR: 'black',
  BOX_ROUNDING_RADIUS: 12,
  ANCHOR_W: 4
});
