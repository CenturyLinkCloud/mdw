import React, {Component} from '../../node/node_modules/react';
import PropTypes from '../../node/node_modules/prop-types';
import {Scrollbars} from '../../node/node_modules/react-custom-scrollbars';
import Toolbar from './Toolbar.jsx';
import '../../node/node_modules/style-loader!./filepanel.css';

class FileView extends Component {
  constructor(...args) {
    super(...args);
    let optsStr = localStorage.getItem('filepanel-options');
    if (optsStr) {
      this.options = JSON.parse(optsStr);
    }
    else {
      this.options = {};
    }
    // default options
    this.options.bufferSize = FileView.BUFFER_SIZE;  // TODO: options
    this.options.fetchThreshold = FileView.FETCH_THRESHOLD; // TODO: options
    
    this.state = {item: {}, buffer: {length: 0}, lineIndex: 0};
    this.fetchLines = this.fetchLines.bind(this);
    this.handleScroll = this.handleScroll.bind(this);
    this.handleOptions = this.handleOptions.bind(this);
  }
  
  componentDidMount() {
  }
  
  componentWillReceiveProps(props) {
    // retrieve fileView
    if (props.item.path) {
      this.setState({
        item: props.item,
        buffer: {length: 0},
        lineIndex: 0
      });
      this.fetchLines(props);
    }
  }
  
  fetchLines(props) {
    let url = this.context.serviceRoot + '/com/centurylink/mdw/system/filepanel';
    url += '?path=' + encodeURIComponent(props.item.path);
    url += '&lineIndex=' + this.state.lineIndex;
    if (this.options.bufferSize) {
      url += '&bufferSize=' + this.options.bufferSize;
    }
    fetch(new Request(url, {
      method: 'GET',
      headers: { Accept: 'application/json'},
      credentials: 'same-origin'
    }))
    .then(response => {
      return response.json();
    })
    .then(json => {
      this.setState({
        item: json.info,
        buffer: json.buffer,
        lineIndex: this.state.lineIndex
      });
      this.props.onInfo(json.info);
    });
  }
  
  handleScroll(values) {
    let lineIndex = Math.round(values.scrollTop * this.state.buffer.length / values.scrollHeight);
    this.setState({
      item: this.state.item,
      lines: this.state.lines,
      lineIndex: lineIndex
    });
    console.log("SCROLL: " + JSON.stringify(values, null, 2));
  }
  
  handleOptions(options) {
    this.options = options;
    localStorage.setItem('filepanel-options', JSON.stringify(options));
    this.setState({
      item: this.state.item,
      lines: this.state.lines,
      lineIndex: this.state.lineIndex
    });
  }
  
  render() {
    var lineNumbers = null;
    if (this.state.buffer.length && this.state.item.isFile && !this.state.item.binary) {
      if (this.options.lineNumbers) {
        lineNumbers = '';
        for (let i = this.state.buffer.start + 1; i < this.state.buffer.length + 1; i++) {
          lineNumbers += i;
          if (i < this.state.buffer.length)
            lineNumbers += '\n';
        }
//        console.log("LINE_NUMS: " + lineNumbers);
      }
    }
    
    return (
      <div className="fp-file-view">
        <Toolbar
          line={this.state.lineIndex + 1}
          item={this.state.item}
          onOptions={this.handleOptions} />
        {this.state.item.isFile &&
          <div className="fp-file">
            <Scrollbars 
              className="fp-file-scroll"
              onScrollFrame={this.handleScroll}>
              <div>
                {lineNumbers &&
                  <div id="fp-line-numbers" className="fp-line-numbers">
                    {lineNumbers}
                  </div>
                }
                <div id="fp-file-content" className="fp-file-content">
                  {this.state.buffer.lines}
                </div>
              </div>
            </Scrollbars>
          </div>
        }
      </div>
    );
  }
}

FileView.contextTypes = {
  hubRoot: PropTypes.string,
  serviceRoot: PropTypes.string
};

FileView.BUFFER_SIZE = 1000;
FileView.FETCH_THRESHOLD = 100;

export default FileView; 