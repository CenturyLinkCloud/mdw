import React, {Component} from '../node/node_modules/react';
import PropTypes from '../node/node_modules/prop-types';
import {Button, Modal} from '../node/node_modules/react-bootstrap';

class Confirm extends Component {
  constructor(...args) {
    super(...args);
    this.state = {show: false, message: ''};
  }

  close(result) {
    this.setState({show: false, message: ''});
    if (this.props.onClose) {
      this.props.onClose(result);
    }
  }

  open(message) {
    this.setState({show: true, message: message});
  }

  render() {
    return (
      <div>
        <Modal show={this.state.show} onHide={() => this.close()}>
          <Modal.Header closeButton>
            <Modal.Title>{this.props.title}</Modal.Title>
          </Modal.Header>
          <Modal.Body>{this.state.message}</Modal.Body>
          <Modal.Footer>
            <Button variant="secondary" onClick={() => this.close(0) }>
              Cancel
            </Button>
            <Button variant="primary" onClick={() => this.close(1)}>
              OK
            </Button>
          </Modal.Footer>
        </Modal>
      </div>
    );  
  }
}

Confirm.propTypes = {
  title: PropTypes.string.isRequired,
  onClose: PropTypes.func
};

export default Confirm;