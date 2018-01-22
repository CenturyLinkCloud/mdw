import React from '../../node/node_modules/react';
import PropTypes from '../../node/node_modules/prop-types';
import {Glyphicon} from '../../node/node_modules/react-bootstrap';
import TreeView from '../../node/node_modules/react-treeview';
import '../../node/node_modules/style-loader!../../react/react-treeview.css';

function DirTree(props, context) {

  const handleClick = event => {
      if (props.onSelect) {
        props.onSelect()
      }
  };

  const selPath = props.selected ? props.selected.path : null;

  const dirLabel = (
    <span 
      className={'fp-item' + (selPath === props.dir.path ? ' fp-selected' : '')}
      tabIndex={props.dir.tabIndex}
      onClick={event => props.onSelect(props.dir)}>
      <Glyphicon glyph="folder-open" className="fp-item-icon" />
      {props.root ? props.dir.path : props.dir.name}
    </span>);
  return (
    <TreeView 
      key={props.dir.path} 
      nodeLabel={dirLabel}
      defaultCollapsed={false}>
      {props.dir.dirs &&
        props.dir.dirs.map(dir => {
          return (
            <DirTree 
              key={dir.name}
              dir={dir} 
              onSelect={props.onSelect} 
              selected={props.selected} />
          );
        })
      }
      {props.dir.files &&
        props.dir.files.map(file => {
          return (
            <div 
              style={{marginTop: '2px'}} // should match tree-view-item margin-top
              key={file.name}>
              <span
                className={'fp-item' + (selPath === file.path ? ' fp-selected' : '')}
                tabIndex={file.tabIndex}
                onClick={event => props.onSelect(file)}>
                <Glyphicon glyph="file" className="fp-item-icon" style={{paddingRight:'4px'}} />
                {file.name}
              </span>
            </div>
          );
        })
      }
    </TreeView>
  );
}
  
DirTree.contextTypes = {
  hubRoot: PropTypes.string,
  serviceRoot: PropTypes.string
};

export default DirTree;