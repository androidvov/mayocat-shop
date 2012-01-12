package org.eschoppe

class Image {

  Integer width
  Integer height
  String extension
  String hint
  byte[] data
  ImageSet imageSet

  static belongsTo = [ImageSet]
  
  static constraints = {
    data(maxSize: MAX_SIZE)
    width nullable:true
    height nullable:true
    extension nullable:true
    hint nullable:true
  }

  static final Integer MAX_SIZE = 2 * 1024 * 1024
}
