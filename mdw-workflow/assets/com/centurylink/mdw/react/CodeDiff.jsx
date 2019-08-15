import React, {Component} from '../node/node_modules/react';
import PropTypes from '../node/node_modules/prop-types';
import ply from '../node/node_modules/ply-ct';
import Compare from './Compare.jsx';
import '../node/node_modules/style-loader!./code-block.css';

const compare = ply.compare;

class CodeDiff extends Component {

  constructor(...args) {
    super(...args);
  }

  render() {
    var diffs = [];
    let mirroredDiffs;
    if (this.props.oldContent) {
      diffs = compare.diffLines(this.props.newContent, this.props.oldContent, null, {
        newlineIsToken: false, 
        ignoreWhitespace: false
      });
      mirroredDiffs = compare.mirrorDiffs(diffs);
    }
    
    const lines = this.props.newContent.replace(/\r/g, '').split(/\n/);
    
    return (
      <div className='side-by-side'>
        {this.props.newContent &&
          <div>
            <div className='code-compare'>
              <label>{this.props.newLabel}</label>
              <Compare 
                code={this.props.newContent} 
                language={this.props.language}
                lines={lines}
                diffs={diffs} diffClass='changes-diff' 
              />
            </div>
          </div>
        }
        {this.props.oldContent &&
          <div className='code-compare'>
            <label>{this.props.oldLabel}</label>
            <Compare 
              code={this.props.oldContent} 
              language={this.props.language}
              lines={this.props.oldContent.replace(/\r/g, '').split(/\n/)}
              diffs={mirroredDiffs} diffClass='changes-diff' />
          </div>
        }
      </div>
    );
  }
}

CodeDiff.propTypes = {
  newLabel: PropTypes.string.isRequired,
  newContent: PropTypes.string,
  oldLabel: PropTypes.string.isRequired,
  oldContent: PropTypes.string,
  language: PropTypes.string.isRequired
};

export default CodeDiff;