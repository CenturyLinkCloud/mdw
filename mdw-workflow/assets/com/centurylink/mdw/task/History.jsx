import React, { Component } from '../node/node_modules/react';
import PropTypes from '../node/node_modules/prop-types';
import Heading from './Heading.jsx';
import UserDate from '../react/UserDate.jsx';

class History extends Component {
  constructor( ...args ) {
    super( ...args );
    this.state = { taskHistory: [] };
  }

  componentDidMount() {
    fetch( new Request( '/mdw/services/Tasks/' + this.props.task.id + '/history', {
      method: 'GET',
      headers: { Accept: 'application/json' }
    } ) )
      .then( response => {
        return response.json();
      } )
      .then( json => {
        this.setState( {
          taskHistory: json.taskHistory
        } );
      } );
  }

  render() {
    return (
      <div>
        <Heading task={this.props.task} refreshTask={this.props.refreshTask} />
        <ul className="mdw-checklist">
          {this.state.taskHistory.map( history => {
            return (
              <li key={history.id}>
                <HistoryItem history={history} />
              </li>
            );
          } )}
        </ul>
      </div>
    );
  }
}

function HistoryItem( props, context ) {
  var history = props.history;
  return (
    <div className="mdw-flex-item">
      <div className="mdw-flex-item-left">
        <div>
          <div>
            <div className="mdw-item-sub" style={{ height: '16px' }}>
              <label>Action:</label>
              {history.eventName}
              <span>{',   '}
                <UserDate label="Created" date={history.createDate} />
              </span>
              <span>{',   '}
                <img src={context.hubRoot + '/images/user.png'} alt="user" />
                {' '}<a className="mdw-link" href={context.hubRoot + '#/users/' + history.createUser}>{history.createUser}</a>
              </span>
            </div>
            <div className="mdw-item-sub">
              {history.comment &&
                <span>
                  <label>Comments:</label>
                  {history.comment}
                </span>
              }
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

History.contextTypes = HistoryItem.contextTypes = {
  hubRoot: PropTypes.string
};

export default History; 