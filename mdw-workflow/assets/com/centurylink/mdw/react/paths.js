module.exports = {
  /**
   * Trims method and dynamic params
   */
  trim: function(path) {
    var p = path;
    const ar = p.indexOf('->');
    if (ar >= 0) {
      p = p.substring(ar + 2);
    }
    const cu = p.indexOf('/{');
    if (cu > 0) {
      p = p.substring(0, cu);
    }
    return p;
  }
};