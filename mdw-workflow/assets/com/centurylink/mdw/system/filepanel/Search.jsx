import React, {Component} from '../../node/node_modules/react';
import PropTypes from '../../node/node_modules/prop-types';
import '../../node/node_modules/style-loader!./filepanel.css';

class Search extends Component {
  constructor(...args) {
    super(...args);
  }
  
  componentDidMount() {
  }
  
  render() {
    return (
      <div className="fp-search">
      </div>
    );
  }
}

Search.contextTypes = {
  hubRoot: PropTypes.string,
  serviceRoot: PropTypes.string
};

export default Search;  