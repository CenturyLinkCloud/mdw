import React, {Component} from '../../node/node_modules/react';
import PropTypes from '../../node/node_modules/prop-types';
import {Scrollbars} from '../../node/node_modules/react-custom-scrollbars';
import '../../node/node_modules/style-loader!./filepanel.css';

class FileView extends Component {
  constructor(...args) {
    super(...args);
    this.state = {item: {}, lines: ''};
    this.handleScroll = this.handleScroll.bind(this);    
  }
  
  componentDidMount() {
    console.log("item: " + this.props.item.path);
  }
  
  componentWillReceiveProps(props) {
    // retrieve fileView
    // call back to handleSelect
    if (props.item.path) { 
      console.log("ITEM: " + JSON.stringify(props.item, null, 2));
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
  
  render() {
    return (
        <Scrollbars 
          className="fp-file-view"
          onScrollFrame={this.handleScroll}>
          {this.state.lines}
        </Scrollbars>
    );
  }
}

FileView.contextTypes = {
  hubRoot: PropTypes.string,
  serviceRoot: PropTypes.string
};

export default FileView; 