import React, {Component} from '../../node/node_modules/react';
import PropTypes from '../../node/node_modules/prop-types';
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
    return (
      <pre className="fp-file-view">
        {JSON.stringify(this.state.item, null, 2)}
      </pre>
    );
  }
}

FileView.contextTypes = {
  hubRoot: PropTypes.string,
  serviceRoot: PropTypes.string
};

export default FileView; 