import React, {Component} from '../../node/node_modules/react';
import PropTypes from '../../node/node_modules/prop-types';
import {Scrollbars} from '../../node/node_modules/react-custom-scrollbars';
import '../../node/node_modules/style-loader!./filepanel.css';

class FileView extends Component {
  constructor(...args) {
    super(...args);
    this.state = {item: {}}
  }
  
  componentDidMount() {
    console.log("item: " + this.props.item.path);
  }
  
  componentWillReceiveProps(props) {
    this.setState({
      item: props.item
    });
  } 
  
  render() {
    var content = '';
    for (var i = 0; i < 100; i++)
      content += '\n' + JSON.stringify(this.state.item, null, 2)
    return (
        <Scrollbars 
          className="fp-file-view">
          {content}
        </Scrollbars>
    );
  }
}

FileView.contextTypes = {
  hubRoot: PropTypes.string,
  serviceRoot: PropTypes.string
};

export default FileView; 