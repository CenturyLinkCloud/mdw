module.exports = {
  graph: {
    nodes: {
      shape: 'dot'
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
        blockShifting: true,
        edgeMinimization: false,
        parentCentralization: true

      }
    },
    physics: {
      enabled: false
    }
  }
};