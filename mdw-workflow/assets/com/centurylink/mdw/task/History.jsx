import React, { Component } from '../node/node_modules/react';

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
        });
  }

  render() {
    var rows = [];
    rows.id = "1";
    this.state.taskHistory.map((history) => {   
      rows.push(<TableRow history = {history} key = {history.id} />);
    });
    return (<div>
     <Table data = {rows} key = {rows.id} />
    </div> );
  }
}

function Table(props) {
  return (
      <table className = "mdw_gridLine">
        <thead>
          <tr>
            <th>Date/Time</th>
            <th>Action</th>
            <th>User</th>
            <th>Comments</th>
          </tr>
        </thead>
        <tbody>{props.data}</tbody>
      </table>
  );  
}

function TableRow(props) {
  return (
      <tr className = "mdw_columnarRow2">
        <td>{props.history.createDate}</td>
        <td>{props.history.eventName}</td>
        <td>{props.history.createUser}</td>
        <td>{props.history.comment}</td>
      </tr>
    );
}

export default History; 