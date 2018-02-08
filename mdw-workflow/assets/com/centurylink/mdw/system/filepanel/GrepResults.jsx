import React from '../../node/node_modules/react';
import PropTypes from '../../node/node_modules/prop-types';
import {Scrollbars} from '../../node/node_modules/react-custom-scrollbars';
// import {Scrollbars} from '../../../../../../../../react-custom-scrollbars';
import '../../node/node_modules/style-loader!./filepanel.css';

function ResultLine(props) {

  const lineMatch = props.lineMatch;
  
  var pos = 0;
  var segments = [];
  lineMatch.matches.forEach((match, i) => {
    if (match.start > pos) {
      segments.push({
        text: lineMatch.line.substring(pos, match.start)
      });
    }
    segments.push({
      text: lineMatch.line.substring(match.start, match.end),
      match: match
    });
    pos = match.end;
  });
  if (lineMatch.line.length > pos + 1) {
    segments.push({
      text: lineMatch.line.substring(pos)
    });
  }
  
  return (
    <span key={lineMatch.index}>
      {'\n'}
      <span className="fp-grep-linenum">
        {((lineMatch.index + 1) + ': ').padStart(props.pad)}
      </span>
      {
        segments.map((segment, i) => {
          return (
            <span key={i}>
              {!segment.match &&
                segment.text
              }
              {segment.match &&
                <a className="fp-grep-hit"
                  href="#/system/filepanel"
                  onClick={e => props.onResultClick(props.file, segment.match)}>
                  {segment.text}
                </a>
              }
            </span>
          );
        })
      }
    </span>
  );
}

function GrepResults(props) {

  var path = props.item.path;
  if (props.item.isFile) {
    path = path.substring(0, path.length - props.item.name.length)
  }

  var pad = 2;
  if (props.results) {
    pad += props.results.reduce((max, result) => {
      return result.lineMatches.reduce((max, lineMatch) => {
        let len = lineMatch.index.toString().length;
        return len > max ? len : max; 
      }, max);
    }, 0);
  }
  
  return (
    <Scrollbars
      className="fp-scroll"
      thumbSize={30}>
      <div className="fp-grep-results">
        <div id="fp-grep-results" className="fp-content">
          {path + '\n\n' }
          { props.results && 
            props.results.map(result => {
              return (
                <div key={path + '/' + result.file}>
                  {result.file + ':'}
                  {
                    result.lineMatches.map(lineMatch => {
                      return (
                        <ResultLine key={lineMatch.index}
                          file={path + '/' + result.file}
                          lineMatch={lineMatch}
                          pad={pad} 
                          onResultClick={props.onResultClick} />
                      );
                    })
                  }
                </div>
              );
            })
          }
        </div>
      </div>
    </Scrollbars>
  );
}

export default GrepResults;