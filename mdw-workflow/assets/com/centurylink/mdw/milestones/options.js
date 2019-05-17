module.exports = {
  graph: {
    nodes: {
      shape: 'dot'
    },
    edges: {
      arrows: 'to'
    },
    layout: {
      hierarchical: {
        enabled: true,
        sortMethod: 'directed',
        blockShifting: true,
        edgeMinimization: true,
        parentCentralization: true

      }
    },
    physics: {
      enabled: true
    }
  }
};