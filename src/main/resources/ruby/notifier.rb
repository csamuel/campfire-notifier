require 'rubygems'
require 'tinder'

class Notifier

  def self.notify(email, password, domain, room_name, message)
    begin
    	campfire = Tinder::Campfire.new domain
    	campfire.login email, password
    	room = campfire.find_room_by_name room_name
    	room.paste message
     rescue
     	puts "Could not connect to Campfire"
     end
  end
  
end