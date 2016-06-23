function StringBuffer()
{
  this.buffer = [];
}

StringBuffer.prototype.append = function append(toAppend)
{
  this.buffer.push(toAppend);
  return this;
}

StringBuffer.prototype.toString = function toString()
{
  return this.buffer.join("");
}