import React, {Component} from '../../node/node_modules/react';
import PropTypes from '../../node/node_modules/prop-types';
import {Scrollbars} from '../../../../../../../../react-custom-scrollbars';
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
    this.options.waitThreshold = FileView.WAIT_THRESHOLD; // TODO: options
    
    this.state = {item: {}, buffer: {length: 0}};
    this.lineIndex = 0;  // not kept in state
    this.retrieving = false;
    
    this.fetchLines = this.fetchLines.bind(this);
    this.handleScrollEvent = this.handleScrollEvent.bind(this);
    this.handleScroll = this.handleScroll.bind(this);
    this.handleOptions = this.handleOptions.bind(this);
  }
  
  componentDidMount() {
  }
  
  componentWillReceiveProps(props) {
    // retrieve fileView
    if (props.item.path) {
      this.lineIndex = 0;
      this.setState({
        item: props.item,
        buffer: {length: 0}
      });
      this.fetchLines(props);
    }
  }
  
  fetchLines(props) {
    this.retrieving = true;
    let url = this.context.serviceRoot + '/com/centurylink/mdw/system/filepanel';
    url += '?path=' + encodeURIComponent(props.item.path);
    url += '&lineIndex=' + this.lineIndex;
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
      this.lineIndex = this.lineIndex ? this.lineIndex : 0;
      this.setState({
        item: json.info,
        buffer: json.buffer
      });
      this.props.onInfo(json.info);
      this.retrieving = false;
    });
  }

  handleScrollEvent(event) {
    console.log("EVENT");
  }
  
  handleScroll(values) {
    console.log("FRAME");
    this.lineIndex = Math.round(values.scrollTop * this.state.item.lineCount * this.getScale() / values.scrollHeight);
    
    // approaching threshold
    if (!this.retrieving) {
      if (this.needsPreBuffer(this.options.fetchThreshold)) {
        this.fetchLines(this.props);
      }
      else if (this.needsPostBuffer(this.options.fetchThreshold)) {
          this.fetchLines(this.props);
      }
      else {
        console.log("STATE1");
        this.setState({
          item: this.state.item,
          lines: this.state.lines
        });
      }
    }
    else {
      console.log("STATE2");
      this.setState({
        item: this.state.item,
        lines: this.state.lines
      });
    }

    console.log("SCROLL: " + JSON.stringify(values, null, 2));
  }
  
  needsPreBuffer(threshold) {
    const preBuf = this.lineIndex - this.state.buffer.start;
    return preBuf < threshold && this.state.buffer.start > 0;
  }
  
  needsPostBuffer(threshold) {
    const bufferEnd = this.state.buffer.start + this.state.buffer.length;
    const postBuf = bufferEnd - this.lineIndex;
    return postBuf < threshold && bufferEnd < this.state.item.lineCount;
  }
  
  
  handleOptions(options) {
    this.options = options;
    localStorage.setItem('filepanel-options', JSON.stringify(options));
    this.setState({
      item: this.state.item,
      lines: this.state.lines
    });
  }
  
  getLineNumbers() {
    var lineNumbers = null;
    if (this.state.buffer.length && this.state.item.isFile && !this.state.item.binary) {
      if (this.options.lineNumbers) {
        lineNumbers = '';
        for (let i = this.state.buffer.start + 1; i < this.state.buffer.length + 1; i++) {
          lineNumbers += i;
          if (i < this.state.buffer.length)
            lineNumbers += '\n';
        }
      }
    }
    return lineNumbers;
  }
  
  getScale() {
    var scale = 1;
    if (this.state.item.lineCount > this.state.buffer.length) {
      scale = this.state.buffer.length / this.state.item.lineCount;
    }
    console.log("SCALE: " + scale);
    return scale;
  }
  
  render() {
    const lineNumbers = this.getLineNumbers();
    
    return (
      <div className="fp-file-view">
        <Toolbar
          line={this.lineIndex + 1}
          item={this.state.item}
          onOptions={this.handleOptions} />
        {this.state.item.isFile &&
          <div className="fp-file">
            <Scrollbars 
              className="fp-file-scroll"
              onScroll={this.handleScrollEvent}
              onScrollFrame={this.handleScroll}
              verticalScale={this.getScale()}>
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

FileView.BUFFER_SIZE = 500;
FileView.FETCH_THRESHOLD = 200;
FileView.WAIT_THRESHOLD = 100; // stop scrolling until available

export default FileView; 