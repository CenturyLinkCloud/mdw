import React, {Component} from '../../node/node_modules/react';
import PropTypes from '../../node/node_modules/prop-types';
import {Scrollbars} from '../../node/node_modules/react-custom-scrollbars';
import Toolbar from './Toolbar.jsx';
import '../../node/node_modules/style-loader!./filepanel.css';

class FileView extends Component {
  constructor(...args) {
    super(...args);
    this.state = {item: {}, buffer: {}};
    this.handleScroll = this.handleScroll.bind(this);
    this.handleOptions = this.handleOptions.bind(this);
  }
  
  componentDidMount() {
  }
  
  componentWillReceiveProps(props) {
    // retrieve fileView
    if (props.item.path) {
      // clear while loading
      this.setState({
        item: props.item,
        lines: ''
      });
      
      let url = this.context.serviceRoot + '/com/centurylink/mdw/system/filepanel?';
      url += 'path=' + encodeURIComponent(props.item.path);  // TODO lineIndex param
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
          buffer: json.buffer
        });
        this.props.onInfo(json.info);
      });
    }
  }
  
  handleScroll(values) {
    console.log("SCROLL: " + JSON.stringify(values, null, 2));
  }
  
  handleOptions(options) {
    localStorage.setItem('filepanel-options', JSON.stringify(options));
    this.setState({
      item: this.state.item,
      lines: this.state.lines
    });
  }
  
  render() {
    var lineIndex = 0;
    var bufferSize = 1000;  // TODO: options
    
    var lineNumbers = null;
    if (this.state.buffer.length && this.state.item.isFile && !this.state.item.binary) {
      var options = localStorage.getItem('filepanel-options');
      if (options) {
        options = JSON.parse(options);
        if (options.lineNumbers) {
          lineNumbers = '';
          for (let i = lineIndex + 1; i < this.state.buffer.length + 1; i++) {
            lineNumbers += i;
            if (i < this.state.buffer.length)
              lineNumbers += '\n';
          }
        }
      }
    }
    
    return (
      <div className="fp-file-view">
        <Toolbar
          line={lineIndex + 1}
          item={this.state.item}
          onOptions={this.handleOptions} />
        {this.state.item.isFile &&
          <div className="fp-file">
            <Scrollbars 
              className="fp-file-scroll"
              onScrollFrame={this.handleScroll}>
              <div>
                {lineNumbers &&
                  <div className="fp-line-numbers">
                    {lineNumbers}
                  </div>
                }
                <div className="fp-file-content">
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

export default FileView; 