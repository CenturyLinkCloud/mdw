module.exports = {
  graph: {
    nodes: {
      shape: 'dot',
      size: 30,
      font: {
        size: 16
      }
    },
    edges: {
      arrows: 'to',
      smooth: {
        type: 'cubicBezier',
        forceDirection: 'vertical',
        roundness: 0.4
      }      
    },
    layout: {
      hierarchical: {
        enabled: true,
        sortMethod: 'directed',
        nodeSpacing: 125,
        blockShifting: true,
        edgeMinimization: true,
        parentCentralization: true

      }
    },
    physics: {
      enabled: false
    }
  }
};