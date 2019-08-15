import React, {Component} from '../node/node_modules/react';
import ButtonLink from '../react/ButtonLink.jsx';
import MdwContext from '../react/MdwContext';

/**
 * Staging area not yet created for user.
 */
class NoStage extends Component {
    
  constructor(...args) {
    super(...args);
  }  

  componentDidMount() {
  }

  render() {
    const hubRoot = this.context.hubRoot;
    return (
      <div style={{padding:'10px'}}>
        <p>
          Staging area not found for {this.context.authUser.name}.
        </p>
        <ButtonLink style={{padding:'4px 8px',fontWeight:'normal',textDecoration:'none'}} 
          to={hubRoot + '/staging/' + this.context.authUser.cuid}>
          Create Staging Area
        </ButtonLink>        
      </div>
    );
  }
}

NoStage.contextType = MdwContext;
export default NoStage; 