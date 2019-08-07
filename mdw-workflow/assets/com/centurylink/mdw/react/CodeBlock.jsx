import React, {Component} from '../node/node_modules/react';
import ReactDOM from '../node/node_modules/react-dom';
import low from '../node/node_modules/lowlight/lib/core';
import '../node/node_modules/style-loader!./code-block.css';

class CodeBlock extends Component {
  
  constructor(...args) {
    super(...args);
    this.mapChild = this.mapChild.bind(this);
    this.mapWithDepth = this.mapWithDepth.bind(this);
    this.markNodes = this.markNodes.bind(this);
    this.assign = this.assign.bind(this);
  }
  
  componentDidUpdate() {
    if (this.props.code && this.props.lineNumbers) {
      var node = ReactDOM.findDOMNode(this); // eslint-disable-line react/no-find-dom-node
      var lineNumElem = node.firstChild;
      var codeElem = node.getElementsByClassName('code-content')[0].firstChild;
      var overflow = codeElem.scrollWidth > codeElem.clientWidth;
      if (overflow) {
        lineNumElem.style.paddingBottom = '22px';
      }
    }
  }
  
  mapChild(child, i, depth) {
    if (child.tagName) {
      return React.createElement(
        child.tagName,
        this.assign({key: 'lo-' + depth + '-' + i}, child.properties),
        child.children && child.children.map(this.mapWithDepth(depth + 1))
      );
    }
    return child.value;
  }

  mapWithDepth(depth) {
    var _this = this;
    return function mapChildrenWithDepth(child, i) {
      return _this.mapChild(child, i, depth);
    };
  }

  assign(dst, src) {
    for (var key in src) {
      dst[key] = src[key];
    }
    return dst;
  }
  
  markNodes(ast, markers) {
    return ast.reduce((nodes, node) => {
      if (node.type === 'text') {
        var text = node.value;
        var processed = null;
        var textEnd = this.idx + text.length;
        var curPos = 0; // latest char position in text covered in output nodes
        markers.forEach((marker, i) => {
          if (marker.start >= marker.end)
            throw new Error('Invalid marker: ' + marker.start + ' >= ' + marker.end);
          if (marker.start >= this.idx && marker.start < textEnd) {
            // mark starts in this node
            if (marker.start > this.idx) {
              // text before mark
              nodes.push({
                type: 'text',
                value: text.substring(curPos, marker.start - this.idx)
              });
              curPos = marker.start - this.idx;
            }
            var markEnd = marker.end >= textEnd ? textEnd : marker.end - this.idx;
            nodes.push({
              type: 'element',
              tagName: 'span',
              properties: {className: marker.className},
              children: [{
                type: 'text',
                value: text.substring(curPos, markEnd)
              }]
            });
            curPos = markEnd;
            processed = marker;
          }
          if (marker.end >= this.idx && marker.end <= textEnd) {
            // mark ends in this node
            if (marker.start < this.idx) {
              // mark started in previous node -- continue
              nodes.push({
                type: 'element',
                tagName: 'span',
                properties: {className: marker.className},
                children: [{
                  type: 'text',
                  value: text.substring(curPos, marker.end - this.idx)
                }]
              });
              curPos = marker.end - this.idx;
            }
            // text after mark
            var end = textEnd;
            // look ahead to see where next marker starts
            if (markers.length > i + 1) {
              var nextMarker = markers[i + 1];
              if (nextMarker.start < textEnd)
                end = nextMarker.start - this.idx;
            }            
            nodes.push({
              type: 'text',
              value: text.substring(curPos, end)
            });
            curPos = end;
            processed = marker;
          }
          if (marker.start < this.idx && marker.end > textEnd) {
            // mark encompasses this node
            nodes.push({
              type: 'element',
              tagName: 'span',
              properties: {className: marker.className},
              children: [{
                type: 'text',
                value: text
              }]
            });
            processed = marker;
          }
        });
        if (!processed) {
          nodes.push(node);
        }
        this.idx = textEnd;
      }
      else {
        node.children = this.markNodes(node.children, markers);
        nodes.push(node);
      }
      return nodes;
    }, []);
  }
  
  applyMarkers(ast, markers) {
    this.idx = 0;
    return this.markNodes(ast, markers);
  }
  
  render() {
    var lineNumbers = '';
    var code = '';
    var lineNumsStyle = {};
    if (this.props.code) {
      if (this.props.lineNumbers) {
        this.props.code.replace(/\n$/, '').split(/\n/).forEach((l, i, arr) => {
          lineNumbers = lineNumbers + (i+1) + '\n';
          if (i === arr.length - 1) {
            var w = 41 + 8 * ((i+1).toString().length - 1);
            lineNumsStyle.width = w;
          }
        });
      }
      var ast = low.highlight(this.props.language, this.props.code).value;
      if (this.props.markers) {
        ast = this.applyMarkers(ast, this.props.markers);        
      }
      var codeProps = {className: 'hljs ' + this.props.language};
      code = React.createElement('code', codeProps, ast.map(this.mapWithDepth(0)));
    }
    return (
      <div className={this.props.className}>
        {lineNumbers &&
          <pre className='line-numbers' style={lineNumsStyle}>{lineNumbers}</pre>
        }
        {this.props.code &&
          <pre className='code-content'>{code}</pre>
        }
      </div>
    );
  }
}

export default CodeBlock;
