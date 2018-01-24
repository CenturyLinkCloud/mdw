import React, {Component} from '../../node/node_modules/react';
import PropTypes from '../../node/node_modules/prop-types';
import {Scrollbars} from '../../node/node_modules/react-custom-scrollbars';
// import {Scrollbars} from '../../../../../../../../react-custom-scrollbars';
import Toolbar from './Toolbar.jsx';
import DirListing from './DirListing.jsx';
import '../../node/node_modules/style-loader!./filepanel.css';

class FileView extends Component {
  constructor(...args) {
    super(...args);

    this.options = Toolbar.getOptions();
    
    this.state = {item: {}, buffer: {length: 0}, search: {results: []}};
    this.lineIndex = 0;  // not kept in state
    this.retrieving = false;
    this.specifiedLineIndex = null; // transient value when click track or drag thumb
    
    this.doFetch = this.doFetch.bind(this);
    this.handleScroll = this.handleScroll.bind(this);
    this.handleOptions = this.handleOptions.bind(this);
    this.handleAction = this.handleAction.bind(this);
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
        buffer: {length: 0},
        search: {results: []}
      });
      this.doFetch(props);
    }
  }
  
  handleOptions(options) {
    this.options = options;
    this.setState({
      item: this.state.item,
      buffer: this.state.buffer,
      search: this.state.search
    });
  }
  
  handleAction(action, params) {
    if (action === 'refresh') {
      this.lineIndex = 0;
      this.setViewScrollTop(0);
      this.doFetch(this.props);
    }
    else if (action === 'download') {
      location = this.context.serviceRoot + 
          '/com/centurylink/mdw/system/filepanel?path=' + 
          encodeURIComponent(this.props.item.path) + '&download=true';
    }
    else if (action === 'scrollToEnd') {
      this.setViewScrollTop(1);
    }
    else if (action === 'search') {
      var search = Object.assign({}, this.state.search, params);
      this.search(search);
    }
    else if (action === 'tailMode') {
      alert('Tail Mode is coming in mdw build 6.0.12');
    }
  }
  
  doFetch(props) {
    this.retrieving = true;
    let url = this.context.serviceRoot + '/com/centurylink/mdw/system/filepanel';
    url += '?path=' + encodeURIComponent(props.item.path);
    if (props.item.isFile) {
      url += '&lineIndex=' + this.lineIndex;
      if (this.options.bufferSize) {
        url += '&bufferSize=' + this.options.bufferSize;
      }
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
      
      if (json.info.isFile) {
        this.setState({
          item: json.info,
          buffer: json.buffer,
          search: {results: []}
        });
        
        // adjust for newly-retrieved lines
        if (this.scrollbars && this.lineIndex) {
          const st = (this.lineIndex - json.buffer.start) * this.scrollbars.view.scrollHeight / (json.info.lineCount * this.getScale());
          this.scrollbars.view.scrollTop = st;
        }
      }
      else {
        // dir listing
        this.setState({
          item: json.info
        })
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
        buffer: this.state.buffer,
        search: this.state.search
      });
      return;
    }
    
    // approaching threshold
    if (!this.retrieving) {
      if (this.needsPreBuffer()) {
        this.doFetch(this.props);
      }
      else if (this.needsPostBuffer()) {
        this.doFetch(this.props);
      }
      else {
        this.setState({
          item: this.state.item,
          buffer: this.state.buffer,
          search: this.state.search
        });
      }
    }
    else {
      this.setState({
        item: this.state.item,
        buffer: this.state.buffer,
        search: this.state.search
      });
    }
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
            for (let i = endIdx; i < clientLines - 1; i++) {
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
    this.scrollbars.view.scrollTop = 
      Math.round((this.specifiedLineIndex - this.state.buffer.start) * scrollHeight / (this.state.item.lineCount * this.getScale()));
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
  
  search(search) {
    search.results = [];
    if (this.state.buffer.length > 0) {
      // start is current search char index
      var start = this.state.search.start;
      if (!start) {
        // start at beginning of top line
        start = 0;
        if (this.lineIndex && this.state.buffer.lines) {
          const bufferLines = this.state.buffer.lines.replace(/\n$/, '').split(/\n/);
          for (let i = this.state.buffer.start; i < this.lineIndex; i++) {
            start += bufferLines[i].length + 1;
          }
        }
      }
      // begin is beginning of buffer lines
      var begin = 0;
      var idx;
      var str = this.state.buffer.lines.toLowerCase();
      var find = search.find.toLowerCase();
      var current;
      while ((idx = str.indexOf(find, begin)) > -1) {
        if (idx > begin) {
          search.results.push({
            text: this.state.buffer.lines.substring(begin, idx)
          });
        }
        begin = idx + find.length;
        if (!current && (!start || start < idx)) {
          current = idx;
        }
        search.results.push({
          index: idx,
          found: this.state.buffer.lines.substring(idx, begin)
        });
      }
      search.start = current;
      if (begin < this.state.buffer.lines.length) {
        search.results.push({
          text: this.state.buffer.lines.substring(begin)
        });
      }
    }
    
    // console.log("RESULTS: " + JSON.stringify(search.results, null, 2));
    if (!search.results.length && this.state.buffer.lineCount > this.state.buffer.start + this.state.buffer.length) {
      // TODO fetch
    }
    else {
      this.setState({
        item: this.state.item,
        buffer: this.state.buffer,
        search: search
      });
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
      <div className="fp-view">
        {this.state.item.isFile &&
          <Toolbar 
            line={this.lineIndex + 1}
            item={this.state.item}
            searchResults={this.state.search.results}
            onOptions={this.handleOptions}
            onAction={this.handleAction} />
        }
        {this.state.item.isFile &&
          <div className="fp-file">
            <Scrollbars ref={sb => { this.scrollbars = sb; }}
              className="fp-scroll"
              thumbSize={FileView.THUMB_SIZE}
              onScrollFrame={this.handleScroll}
              onVerticalTrackClick={this.handleVerticalTrackClick}
              onVerticalDrag={this.handleVerticalDrag}
              thumbVerticalY={thumbVerticalY}
              hideTracksWhenNotNeeded={true}
              universal={true}>
              <div>
                {lineNumbers &&
                  <div id="fp-line-numbers" className="fp-line-numbers">
                    {lineNumbers}
                  </div>
                }
                {this.state.search.results.length == 0 &&
                  <div id="fp-file-content" className="fp-content">
                    {this.state.buffer.lines}
                  </div>
                }
                {this.state.search.results.length > 0 &&
                  <div id="fp-file-content" className="fp-content">
                    {
                      this.state.search.results.map((res, i) => {
                        return (
                          <span key={i}>
                            {res.text &&
                              <span>{res.text}</span>
                            }
                            {res.found &&
                              <span id={'res-' + res.index} className={this.state.search.start === res.index ? 'fp-current' : 'fp-mark'}>{res.found}</span>
                            }
                          </span>
                        );
                      })
                    }
                  </div>
                }
              </div>
            </Scrollbars>
          </div>
        }
        {!this.state.item.isFile && this.state.item.path &&
          <div className="fp-dir">
            <DirListing item={this.state.item} />
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

FileView.THUMB_SIZE = 30;
// must match css
FileView.FONT_SIZE = 13;
FileView.LINE_HEIGHT = 1.3;

export default FileView; 