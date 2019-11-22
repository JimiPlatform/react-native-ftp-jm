
Pod::Spec.new do |s|
  s.name         = "react-native-ftp-jm"
  s.version      = "1.0.0"
  s.summary      = "react-native-ftp-jm"
  s.description  = <<-DESC
                  react-native-ftp-jm
                   DESC
  s.homepage     = "https://github.com/CLANNADAIR/react-native-ftp-jm"
  s.license      = "MIT"
  # s.license      = { :type => "MIT", :file => "FILE_LICENSE" }
  s.author             = { "clannad" => "522674616@qq.com" }
  s.platform     = :ios, "9.0"
  s.source       = { :git => "https://github.com/CLANNADAIR/react-native-ftp-jm", :tag => "#{s.version}" }
  s.source_files  = "ios/*"
  s.requires_arc = true

  s.dependency "React"
  s.frameworks = 'UIKit'
  s.frameworks = 'AVFoundation'
  s.frameworks = 'CommonCrypto'
  s.dependency 'React'
  s.dependency 'Alamofire', '4.9.0'
  s.dependency 'FilesProvider', '0.26.0'
  s.dependency 'AFNetworking', '3.2.1'
  s.dependency 'CocoaAsyncSocket', '7.6.3'

  #s.dependency "others"

end

  