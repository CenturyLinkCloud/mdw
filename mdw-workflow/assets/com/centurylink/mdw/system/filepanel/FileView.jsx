import React, {Component} from '../../node/node_modules/react';
import PropTypes from '../../node/node_modules/prop-types';
import {Scrollbars} from '../../node/node_modules/react-custom-scrollbars';
import Toolbar from './Toolbar.jsx';
import '../../node/node_modules/style-loader!./filepanel.css';

class FileView extends Component {
  constructor(...args) {
    super(...args);
    this.state = {item: {}, lines: ''};
    this.handleScroll = this.handleScroll.bind(this);
    this.handleOptions = this.handleOptions.bind(this);
  }
  
  componentDidMount() {
  }
  
  componentWillReceiveProps(props) {
    // retrieve fileView
    // call back to handleSelect
    if (props.item.path) { 
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
        // TODO handleSelect without refresh
        this.setState({
          item: json.info,
          lines: json.lines ? json.lines : ''
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
    var bufferLines = 1000;
    var l = this.state.lines.replace(/\n$/, '').split(/\n/).length;
    if (l < bufferLines) {
      bufferLines = l;
    }
    
    var lineNumbers = null;
    if (this.state.lines.length > 0) {
      var options = localStorage.getItem('filepanel-options');
      if (options) {
        options = JSON.parse(options);
        if (options.lineNumbers) {
          lineNumbers = '';
          for (let i = lineIndex + 1; i < bufferLines + 1; i++) {
            lineNumbers += i;
            if (i < bufferLines)
              lineNumbers += '\n';
          }
        }
        console.log("options: " + JSON.stringify(options, null, 2));
      }
    }
    
    return (
        <div style={{height:'100%'}}>
          <Toolbar onOptions={this.handleOptions} />
          <div className="fp-file">
            <Scrollbars 
              className="fp-file-view"
              onScrollFrame={this.handleScroll}>
              <div>
                {lineNumbers &&
                  <div className="fp-line-numbers">
                    {lineNumbers}
                  </div>
                }
                <div className="fp-file-content">
                  {this.state.lines}
                </div>
              </div>
            </Scrollbars>
          </div>
        </div>
    );
  }
}

FileView.contextTypes = {
  hubRoot: PropTypes.string,
  serviceRoot: PropTypes.string
};

export default FileView; 