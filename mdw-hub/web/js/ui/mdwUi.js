'use strict';

// Global access to ui objects (for react components).
// Drawing objects are pre-injected in admin.js.
var $mdwUi = {
    
   init: function(ngInjector) {
     $mdwUi.Shape = ngInjector.get('Shape');
     $mdwUi.Label = ngInjector.get('Label');
     $mdwUi.Diagram = ngInjector.get('Diagram');
     $mdwUi.Link = ngInjector.get('Link');
     $mdwUi.Step = ngInjector.get('Step');
     $mdwUi.Marquee = ngInjector.get('Marquee');
     $mdwUi.Note = ngInjector.get('Note');
     $mdwUi.Selection = ngInjector.get('Selection');
     $mdwUi.Subflow = ngInjector.get('Subflow');
     $mdwUi.Toolbox = ngInjector.get('Toolbox');
     $mdwUi.Inspector = ngInjector.get('Inspector');
     $mdwUi.InspectorTabs = ngInjector.get('InspectorTabs');
     $mdwUi.Configurator = ngInjector.get('Configurator');
   },
   pseudoImplementors: [
     {
       category: 'subflow',
       label: 'Exception Handler Subflow',
       icon: 'com.centurylink.mdw.base/subflow.png',
       implementorClass: 'Exception Handler'
     },
     {
       category: 'subflow',
       label: 'Cancelation Handler Subflow',
       icon: 'com.centurylink.mdw.base/subflow.png',
       implementorClass: 'Cancelation Handler'
     },
     {
       category: 'subflow',
       label: 'Delay Handler Subflow',
       icon: 'com.centurylink.mdw.base/subflow.png',
       implementorClass: 'Delay Handler'
     },    
     {
       category: 'note',
       label: 'Text Note',
       icon: 'com.centurylink.mdw.base/note.png',
       implementorClass: 'TextNote'
     }
   ],
   DC: {
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
     MIN_DRAG: 3,
     OVAL_LINE_WIDTH: 3,
     HIGHLIGHT_MARGIN: 10,
     HIGHLIGHT_COLOR: '#03a9f4'
   }
};