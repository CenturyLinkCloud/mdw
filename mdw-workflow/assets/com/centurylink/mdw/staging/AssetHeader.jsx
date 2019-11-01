import React, {Component} from '../node/node_modules/react';
import {Button, Glyphicon} from '../node/node_modules/react-bootstrap';
import MdwContext from '../react/MdwContext';

class AssetHeader extends Component {
    
  constructor(...args) {
    super(...args);
  }  

  componentDidMount() {
  }

  render() {
    const hubRoot = this.context.hubRoot;
    return (
      <div className="panel-heading mdw-heading" style={{borderColor:'#ddd'}}>
        <div className="mdw-heading-label">
          <div style={{marginTop:'-5px'}}>
            <a href={hubRoot + '/packages/' + this.props.package} style={{marginRight:'1px'}}
              onClick={e => {e.preventDefault(); location = hubRoot + '/#/packages/' + this.props.package; }}>
              {this.props.package}
            </a>
            {'/' + this.props.assetName}
            {this.props.asset && 
              <span>
                {this.props.asset.version &&
                  <span>{' v' + this.props.asset.version}</span>
                }
                <a title="Raw" style={{marginLeft:'10px'}}
                  href={hubRoot + '/asset/' + this.props.package + '/' + this.props.asset.name + '?stagingUser=' + this.props.stagingCuid}>
                  <Glyphicon glyph="file" className="mdw-item-icon" />
                </a>
                <a title="Download" style={{marginLeft:'-5px'}}
                  href={hubRoot + '/asset/' + this.props.package + '/' + this.props.asset.name + '?download=true&NoPersistence=true&stagingUser=' + this.props.stagingCuid}>
                  <Glyphicon glyph="download-alt" className="mdw-item-icon" />
                </a>
              </span>
            }
          </div>
          {this.props.asset &&
            <div>
              {this.props.asset.commitInfo &&
                <span className="mdw-commit" style={{fontSize:'13px',marginLeft:'0'}}
                title={this.props.asset.commitInfo.message}>
                  {'(' + this.props.asset.commitInfo.committer + '  ' + this.props.asset.commitInfo.date + ')'}
                </span>
              }
            </div>
          }
        </div>
        {this.props.asset && !this.props.asset.isBinary && this.props.asset.vcsDiff &&
          <div className="radio mdw-heading-options" style={{marginTop:'-5px'}}>
            <label className="radio-inline">
              <input type="radio" checked={this.props.view === 'asset'}
                onChange={() => this.props.onViewChange('asset')}/>
              Asset
            </label>
            <label className="radio-inline" checked={this.props.view === 'diff'}>
              <input type="radio" checked={this.props.view === 'diff'}
                onChange={() => this.props.onViewChange('diff')} />
              Diff
            </label>
          </div>
        }
        {this.props.asset &&
          <div className="mdw-heading-actions">
            {this.props.stagingCuid === this.context.authUser.cuid && !this.props.asset.isBinary && 'MISSING' !== this.props.asset.vcsDiffType &&
              <a className="btn btn-primary mdw-action-btn" style={{fontSize:'14px',fontWeight:'normal'}}
                href={hubRoot + '/edit/' + this.props.package + '/' + this.props.asset.name}>
                <Glyphicon glyph="pencil" />
                {' Edit'}
              </a>
            }
            {this.props.stagingCuid === this.context.authUser.cuid && this.props.view !== 'upload' && 'MISSING' !== this.props.asset.vcsDiffType &&
              <Button className="btn btn-primary mdw-btn mdw-action-btn" style={{padding:'4px 6px'}}
                onClick={() => this.props.onViewChange('upload')} title="Upload a new asset version">
                <Glyphicon glyph="download-alt" style={{transform:'rotate(180deg)'}}/>
                {' Upload'}
              </Button>
            }
            {this.props.stagingCuid === this.context.authUser.cuid && 'MISSING' !== this.props.asset.vcsDiffType &&
              <Button className="btn btn-primary mdw-btn mdw-action-btn" style={{padding:'4px 6px'}}
                onClick={this.props.onDelete}>
                <Glyphicon glyph="remove" />
                {' Delete'}
              </Button>
            }
            <Button className="btn btn-primary mdw-btn mdw-action-btn" style={{padding:'4px 6px'}}
              onClick={this.props.onUnstage}>
              <Glyphicon glyph="arrow-left" />
              {' Unstage'}
            </Button>
          </div>
        }
      </div>
    );
  }
}

AssetHeader.contextType = MdwContext;
export default AssetHeader; 