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
    
    this.state = {item: {}, buffer: {length: 0}};
    this.lineIndex = 0;  // not kept in state
    this.retrieving = false;
    this.specifiedLineIndex = null; // transient value when click track or drag thumb
    
    this.fetchLines = this.fetchLines.bind(this);
    this.handleScroll = this.handleScroll.bind(this);
    this.handleOptions = this.handleOptions.bind(this);
    this.needsPreBuffer = this.needsPreBuffer.bind(this);
    this.needsPostBuffer = this.needsPostBuffer.bind(this);
    this.getLineNumbers = this.getLineNumbers.bind(this);
    this.getScale = this.getScale.bind(this);
    this.handleVerticalTrackClick = this.handleVerticalTrackClick.bind(this);
    this.handleVerticalDrag = this.handleVerticalDrag.bind(this);
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
      
      // adjust for newly-retrieved lines
      if (this.scrollbars && this.lineIndex) {
        const st = (this.lineIndex - json.buffer.start) * this.scrollbars.view.scrollHeight / (json.info.lineCount * this.getScale());
        this.scrollbars.view.scrollTop = st;
      }
      
      this.props.onInfo(json.info);
      this.retrieving = false;
    });
  }

  handleScroll(values) {
    if (this.specifiedLineIndex !== null) {
      this.lineIndex = this.specifiedLineIndex;
      this.specifiedLineIndex = null;
    }
    else {
      this.lineIndex = Math.round(values.scrollTop * this.state.item.lineCount * this.getScale() / values.scrollHeight) + this.state.buffer.start;
    }
    
    if (this.retrieving) {
      this.setState({
        item: this.state.item,
        lines: this.state.lines
      });
      return;
    }
    
    // approaching threshold
    if (!this.retrieving) {
      if (this.needsPreBuffer()) {
        this.fetchLines(this.props);
      }
      else if (this.needsPostBuffer()) {
          this.fetchLines(this.props);
      }
      else {
        this.setState({
          item: this.state.item,
          lines: this.state.lines
        });
      }
    }
    else {
      this.setState({
        item: this.state.item,
        lines: this.state.lines
      });
    }
    // console.log("SCROLL: " + JSON.stringify(values, null, 2));    
  }
  
  needsPreBuffer() {
    const preBuf = this.lineIndex - this.state.buffer.start;
    return preBuf < this.options.fetchThreshold && this.state.buffer.start > 0;
  }
  
  needsPostBuffer() {
    const bufferEnd = this.state.buffer.start + this.state.buffer.length;
    const postBuf = bufferEnd - this.lineIndex;
    return postBuf < this.options.fetchThreshold && bufferEnd < this.state.item.lineCount;
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
    if (this.scrollbars) {
      if (this.state.buffer.length && this.state.item.isFile && !this.state.item.binary) {
        if (this.options.lineNumbers) {
          lineNumbers = '';
          const endIdx = this.state.buffer.length + this.state.buffer.start;
          for (let i = this.state.buffer.start + 1; i < endIdx + 1; i++) {
            lineNumbers += i;
            if (i < endIdx)
              lineNumbers += '\n';
          }
          const clientLines = this.getClientLines();
          if (endIdx < clientLines) {
            for (let i = endIdx; i < clientLines + 1; i++) {
              lineNumbers += '\n';
            }
          }
        }
      }
    }
    return lineNumbers;
  }
  
  handleVerticalTrackClick(event) {
    if (this.scrollbars) {
      const {target, clientY} = event;
      const {top: targetTop} = target.getBoundingClientRect();
      var y = clientY - targetTop;
      var frac = y / this.scrollbars.trackVertical.clientHeight;
      this.setViewScrollTop(frac);
    }
  }
  
  handleVerticalDrag(event) {
    const { clientY } = event;
    const { top: trackTop } = this.scrollbars.trackVertical.getBoundingClientRect();
    const clickPosition = FileView.THUMB_SIZE - this.scrollbars.prevPageY;
    var y = clientY - trackTop - clickPosition;
    var frac = y / this.scrollbars.trackVertical.clientHeight;
    if (frac < 0) {
      frac = 0;
    }
    else if (frac > 1) {
      frac = 1;
    }
    if (!this.retrieving) {
      this.setViewScrollTop(frac);
    }
  }
  
  setViewScrollTop(fraction) {
    this.specifiedLineIndex = Math.round(fraction * (this.state.item.lineCount - this.getClientLines()));
    if (this.specifiedLineIndex < 0) {
      this.specifiedLineIndex = 0;
    }
    const scrollHeight = this.scrollbars.view.scrollHeight;
    this.scrollbars.view.scrollTop = Math.round((this.specifiedLineIndex - this.state.buffer.start) * scrollHeight / (this.state.item.lineCount * this.getScale()));
  }
  
  getScale() {
    var scale = 1;
    if (this.state.item.lineCount > this.state.buffer.length) {
      scale = this.state.buffer.length / this.state.item.lineCount;
    }
    return scale;
  }
  
  getClientLines() {
    if (this.scrollbars) {
      const lineHeight = FileView.FONT_SIZE * FileView.LINE_HEIGHT;
      return this.scrollbars.view.clientHeight / lineHeight + 1; // TODO: why off by one?
    }
  }
  
  render() {
    var lineNumbers = this.getLineNumbers();

    if (this.scrollbars) {
      var thumbVerticalY = 0;
      if (this.lineIndex > 0) {
        const values = this.scrollbars.getValues();
  
        const trackVerticalHeight = this.getInnerHeight(this.scrollbars.trackVertical);
        const frac = this.lineIndex / (this.state.item.lineCount - this.getClientLines());
        thumbVerticalY = frac * (trackVerticalHeight - FileView.THUMB_SIZE);
      }
    }
    
    return (
      <div className="fp-file-view">
        <Toolbar 
          line={this.lineIndex + 1}
          item={this.state.item}
          onOptions={this.handleOptions} />
        {this.state.item.isFile &&
          <div className="fp-file">
            <Scrollbars ref={sb => { this.scrollbars = sb; }}
              className="fp-file-scroll"
              thumbSize={FileView.THUMB_SIZE}
              onScrollFrame={this.handleScroll}
              onVerticalTrackClick={this.handleVerticalTrackClick}
              onVerticalDrag={this.handleVerticalDrag}
              thumbVerticalY={thumbVerticalY}>
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

  getInnerHeight(el) {
    const { clientHeight } = el;
    const { paddingTop, paddingBottom } = getComputedStyle(el);
    return clientHeight - parseFloat(paddingTop) - parseFloat(paddingBottom);
  }
}

FileView.contextTypes = {
  hubRoot: PropTypes.string,
  serviceRoot: PropTypes.string
};

FileView.BUFFER_SIZE = 500;
FileView.FETCH_THRESHOLD = 200;
FileView.THUMB_SIZE = 30;
// must match css
FileView.FONT_SIZE = 13;
FileView.LINE_HEIGHT = 1.3;

export default FileView; 