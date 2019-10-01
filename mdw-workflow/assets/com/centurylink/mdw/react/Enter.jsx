import React, {Component} from '../node/node_modules/react';
import PropTypes from '../node/node_modules/prop-types';
import {Button, Modal, ControlLabel} from '../node/node_modules/react-bootstrap';

class Enter extends Component {
  constructor(...args) {
    super(...args);
    this.state = {show: false, message: '', entry: ''};
    this.handleChange = this.handleChange.bind(this);
  }

  close(result) {
    this.setState({show: false, message: '', entry: ''});
    if (this.props.onClose) {
      this.props.onClose(result);
    }
  }

  open(message) {
    this.setState({show: true, message: message});
  }

  handleChange(e) {
    this.setState({show: this.state.show, message: this.state.message, entry: e.currentTarget.value});
  }

  render() {
    return (
      <div>
        <Modal show={this.state.show} onHide={() => this.close()}>
          <Modal.Header closeButton>
            <Modal.Title>{this.props.title}</Modal.Title>
          </Modal.Header>
          <Modal.Body>
            <div>
              {this.state.message}
            </div>
            <div>
              {this.props.label &&
                <ControlLabel style={{marginRight:'5px'}}>
                  {this.props.label}
                </ControlLabel>
              }
              <input type="text" style={{width:'350px',marginTop:'10px'}}
                value={this.state.entry} 
                onChange={this.handleChange} />
            </div>
          </Modal.Body>
          <Modal.Footer>
            <Button variant="secondary" onClick={() => this.close() }>
              Cancel
            </Button>
            <Button variant="primary" onClick={() => this.close(this.state.entry)}>
              OK
            </Button>
          </Modal.Footer>
        </Modal>
      </div>
    );  
  }
}

Enter.propTypes = {
  title: PropTypes.string.isRequired,
  onClose: PropTypes.func
};

export default Enter;