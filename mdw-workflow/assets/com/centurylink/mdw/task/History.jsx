import React, { Component } from '../node/node_modules/react';
import {Table} from '../node/node_modules/react-bootstrap';

class History extends Component {
  constructor( ...args ) {
    super( ...args );
    this.state = { taskHistory: [] };
  }

  componentDidMount() {
    fetch( new Request( '/mdw/services/Tasks/' + this.props.task.id + '/history', {
      method: 'GET',
      headers: { Accept: 'application/json' }
    }))
      .then( response => {
        return response.json();
      } )
      .then( json => {
        this.setState( {
          taskHistory: json.taskHistory
        });
      });
  }

  render() {
    var rows = [];
    this.state.taskHistory.map((history) => {   
      rows.push(<Row history = {history} key = {history.id} />);
    });
    return (<div>
      <div>
        <label className = "col-xs-3">Date/Time</label>
        <label className = "col-xs-3">Action</label>
        <label className = "col-xs-3">User</label>
        <label>Comments</label>
     </div>
    {rows}
    </div>);
  }
}

function Row(props) {
  return (
      <div>
        <div className = "col-xs-3">{props.history.createDate}</div>
        <div className = "col-xs-3">{props.history.eventName}</div>
        <div className = "col-xs-3">{props.history.createUser}</div>
        <div>{props.history.comment}</div>
      </div>
  );
}

export default History; 