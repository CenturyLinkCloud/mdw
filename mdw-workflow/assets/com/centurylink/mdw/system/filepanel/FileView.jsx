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
    
    this.state = {item: {}, buffer: {length: 0}, search: {results: [], message: null}, tailOn: false};
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
    this.stopTail();
    // retrieve fileView
    if (props.item.path) {
      this.lineIndex = 0;
      this.setState({
        item: props.item,
        buffer: {length: 0},
        search: {results: [], message: null}
      });
      this.doFetch(props);
    }
  }
  
  componentDidUpdate() {
    if (this.beforeRender) {
      console.log('Render time: ' + (Date.now() - this.beforeRender) + ' ms');
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
      delete this.rememberedScrollStart;
      this.setViewScrollTop(0);
      this.doFetch(this.props);
    }
    else if (action === 'download') {
      location = this.context.serviceRoot + 
          '/com/centurylink/mdw/system/filepanel?path=' + 
          encodeURIComponent(this.props.item.path) + '&download=true';
    }
    else if (action === 'scrollToEnd') {
      this.scrollToEnd();
    }
    else if (action === 'find') {
      this.stopTail();
      var search = Object.assign({}, this.state.search, params, {message: null});
      this.find(search);
    }
    else if (action === 'search') {
      this.stopTail();
      var search = Object.assign({}, this.state.search, params, {message: null});
      if (search.find.search) {
        if (this.options.searchWhileTyping) {
          // find() was already run
          this.search(search);
        }
        else {
          this.find(search, () => this.search(search));
        }
      }
      else { // clear
        this.setState({
          item: this.state.item,
          buffer: this.state.buffer,
          search: {results: [], message: null}
        });
      }
    }
    else if (action === 'tailMode') {
      const wasOn = this.state.tailOn;
      this.stopTail();
      this.state.tailOn = !wasOn;
      this.setState(this.state);
      if (!wasOn) {
        // toggling start new tail (after update to reflect latest)
        this.doFetch(this.props, null, () => {
          this.scrollToEnd();
          this.tail(true);
        });
      }
    }
  }
  
  doFetch(props, params, callback) {
    this.retrieving = true;
    $mdwUi.hubLoading(true);
    let url = this.context.serviceRoot + '/com/centurylink/mdw/system/filepanel';
    url += '?path=' + encodeURIComponent(props.item.path);
    if (props.item.isFile) {
      url += '&lineIndex=' + this.lineIndex;
      if (this.options.bufferSize) {
        url += '&bufferSize=' + this.options.bufferSize;
      }
      if (params) {
        url += '&' + params;
      }
    }
    fetch(new Request(url, {
      method: 'GET',
      headers: { Accept: 'application/json'},
      credentials: 'same-origin'
    }))
    .then(response => {
      $mdwUi.hubLoading(false);
      return response.json();
    })
    .then(json => {
      this.lineIndex = this.lineIndex ? this.lineIndex : 0;
      var prevSearchIndex = this.state.buffer.start;
      if (!this.state.search.backward) {
        prevSearchIndex += this.state.buffer.length; 
      }
      if (json.info.isFile) {
        this.setState({
          item: json.info,
          buffer: json.buffer,
          search: {find: this.state.search.find, results: [], message: null, backward: this.state.search.backward}
        });
        
        // adjust for newly-retrieved lines
        if (this.scrollbars && this.lineIndex) {
          const st = (this.lineIndex - json.buffer.start) * this.scrollbars.view.scrollHeight / (json.info.lineCount * this.getScale());
          this.scrollbars.view.scrollTop = st;
        }
        
        // find search pattern if present
        if (this.state.search.find) {
          this.find({find: this.state.search.find, backward: this.state.search.backward});
          var searchIndex = json.info.searchIndex;
          if (typeof(searchIndex) !== 'undefined') {
            if (searchIndex === -1) {
              this.state.search.message = 'Not found';
              this.setState({
                item: this.state.item,
                buffer: this.state.buffer,
                search: this.state.search
              });
              
            }
            else {
              if (this.state.search.backward) {
                if (searchIndex >= prevSearchIndex - 1) {
                  this.state.search.message = 'Wrapped';
                } 
              }
              else {
                if (searchIndex <= prevSearchIndex) {
                  this.state.search.message = 'Wrapped';
                }
              }
              if (this.lineIndex + this.getClientLines() < searchIndex || this.lineIndex > searchIndex) {
                // make lineIndex visible after service search 
                this.lineIndex = searchIndex;
              }
              this.search(this.state.search);
            }
          }
          else if (typeof(this.rememberedSearchStart) !== 'undefined') {
            // may have scrolled for buffering after scroll due to search
            this.search(Object.assign(this.state.search), {start: this.rememberedSearchStart})
          }
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
      if (callback) {
        callback();
      }
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
    
    // unset search start so it'll be recalculated based on current scroll position
    delete this.state.search.start;
    const rememberedSearchStart = this.rememberedSearchStart;
    if (this.rememberedSearchStart !== 'undefined') {
      this.state.search.start = this.rememberedSearchStart;
      delete this.rememberedSearchStart;
    }    
    
    if (this.retrieving) {
      this.setState({
        item: this.state.item,
        buffer: this.state.buffer,
        search: this.state.search
      });
      return;
    }
    else {
      // approaching threshold?
      if (!this.retrieving) {
        if (this.needsPreBuffer()) {
          this.rememberedSearchStart = rememberedSearchStart;
          this.doFetch(this.props);
        }
        else if (this.needsPostBuffer()) {
          this.rememberedSearchStart = rememberedSearchStart;
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
      return (this.scrollbars.view.offsetHeight - 6) / lineHeight;
    }
  }

  scrollToEnd() {
    delete this.rememberedScrollStart;
    this.setViewScrollTop(1);
  }
  
  // finds and highlights (no scroll or 
  find(search, callback) {
    search.results = [];
    if (this.state.buffer.length > 0) {
      // begin is beginning of buffer lines
      var begin = 0;
      var idx;
      var str = this.state.buffer.lines.toLowerCase();
      var find = search.find.toLowerCase();
      while ((idx = str.indexOf(find, begin)) > -1) {
        if (idx > begin) {
          search.results.push({
            text: this.state.buffer.lines.substring(begin, idx)
          });
        }
        begin = idx + find.length;
        search.results.push({
          index: idx,
          found: this.state.buffer.lines.substring(idx, begin)
        });
      }
      
      if (begin < this.state.buffer.lines.length) {
        search.results.push({
          text: this.state.buffer.lines.substring(begin)
        });
      }

      this.setState({
        item: this.state.item,
        buffer: this.state.buffer,
        search: search
      }, callback);
    }    
  }
    
  // Go to next match (assumes find has been executed).
  // If not found in buffer, fetch from server.
  search(search) {
    if (this.state.buffer.length > 0) {
      // start is current search char index within buffer
      var start = this.state.search.start;
      if (typeof(start) === 'undefined') {
        // start at beginning of top line or end of bottom line (for backward)
        start = -1;
        if (this.state.buffer.lines) {
          const bufferLines = this.state.buffer.lines.replace(/\n$/, '').split(/\n/);
          var stop = this.lineIndex - this.state.buffer.start;
          if (search.backward) {
            stop += this.getClientLines() + 1;
            if (stop > this.state.buffer.length) {
              stop = this.state.buffer.length;
            }
          }
          for (let i = 0; i < stop; i++) {
            start += bufferLines[i].length + 1;
          }
          if (search.backward) {
            start++;
          }
        }
      }
      
      if (this.state.search.results) {
        var idx = -1;
        for (let i = 0; i < this.state.search.results.length; i++) {
          const result = this.state.search.results[i];
          if (result.found) {
            if (search.backward) {
              if (start > result.index) {
                idx = i;
              }
            }
            else {
              if (start < result.index) {
                idx = i;
                break;
              }
            }
          }
        }
        if (idx >= 0) {
          // found next match in buffer
          const res = this.state.search.results[idx];
          search.start = res.index;
          this.scrollSearchResultIntoView(search, res);
        }
        else {
          // server search from edge of buffer
          this.state.search.backward = search.backward;
          var searchIndex = this.state.buffer.start;
          if (!search.backward) {
            searchIndex += this.state.buffer.length; 
          }
          var params = 'search=' + search.find + '&searchIndex=' + searchIndex;
          if (search.backward) {
            params += '&backward=true';
          }
          this.doFetch(this.props, params);
        }
      }
    }
  }
  
  scrollSearchResultIntoView(search, result) {
    this.rememberedSearchStart = search.start; // remember search start
    
    var elem = document.getElementById('res-' + result.index);
    if (typeof elem.scrollIntoViewIfNeeded === 'function') {
      elem.scrollIntoViewIfNeeded({behavior: 'instant', block: 'center', inline: 'center'});
    }
    else {
      try {
        elem.scrollIntoView({behavior: 'instant', block: 'center', inline: 'center'});
      }
      catch (err) {
        elem.scrollIntoView();
      }
    }
    
    this.setState({
      item: this.state.item,
      buffer: this.state.buffer,
      search: search
    });
  }
  
  // stop any existing tail
  stopTail() {
    if (this.props.item && this.props.item.path && this.state.tailOn) {  
      this.state.tailOn = false;
      this.tail(false);
    }
  }
  
  tail(tailOn) {
    this.state.tailOn = tailOn;
    const webSocketUrl = $mdwUi.getWebSocketUrl();
    if (webSocketUrl) {
      if (!this.state.tailOn && this.webSocket) {
        this.webSocket.close();
      }
      this.setState(this.state);
      $mdwUi.hubLoading(true);
      let url = this.context.serviceRoot + '/com/centurylink/mdw/system/filepanel';
      url += '?path=' + encodeURIComponent(this.props.item.path);
      url += '&tail=' + this.state.tailOn;
      fetch(new Request(url, {
        method: 'GET',
        headers: { Accept: 'application/json'},
        credentials: 'same-origin'
      }))
      .then(response => {
        $mdwUi.hubLoading(false);
        return response.json();
      })
      .then(responseJson => {
        if (this.state.tailOn) {
          const fileView = this;
          this.webSocket = new WebSocket(webSocketUrl);
            this.webSocket.addEventListener('open', function(event) {
              fileView.webSocket.send(fileView.props.item.path);
            });
            this.webSocket.addEventListener('message', function(event) {
              const json = JSON.parse(event.data);
              console.log("MESSAGE: " + JSON.stringify(json, null, 2));
              if (json.buffer.length) {
                const buffer = fileView.state.buffer;
                // response always repeats the last line in case it changed
                var lines = fileView.state.buffer.lines.split(/\n/);
                const newLength = buffer.length + json.buffer.length - 1;
                if (newLength > fileView.options.bufferSize) {
                  lines.splice(0, newLength - fileView.options.bufferSize);
                }
                else {
                  buffer.length = newLength;
                }
                lines.splice(lines.length - 2, 1);
                buffer.lines = lines.join('\n') + json.buffer.lines;
                fileView.setState({
                  item: json.info,
                  buffer: fileView.state.buffer,
                  search: fileView.state.search
                });
                fileView.scrollToEnd();
              }
              else {
                // file length has diminished -- re-retrieve and stop tailing
                fileView.doFetch(fileView.props, null, () => {
                  fileView.scrollToEnd();
                  fileView.stopTail();
                });
              }
            });
        }
        else if (this.webSocket) {
          delete this.tailPath;
          delete this.webSocket;
        }
      });
    }
  }
  
  render() {
    // uncomment for render timing
    // this.beforeRender = Date.now();

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
            searchMessage={this.state.search.message}
            onOptions={this.handleOptions}
            onAction={this.handleAction} 
            tailMode={this.state.tailOn} />
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