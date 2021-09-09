use std::io::{Read, Write};
use std::net::TcpStream;

const PLAYER_IS_FOUND: u8 = 0;
const PLAYER_MOVED: u8 = 1;

pub(crate) struct ClientPlayer {
    stream: TcpStream,
}

impl ClientPlayer {
    #[inline]
    pub(crate) fn new(ip: String) -> std::io::Result<ClientPlayer> {
        Ok(ClientPlayer {
            stream: TcpStream::connect(format!("{}:1337", ip))?,
        })
    }

    #[inline]
    pub(crate) fn send_ready(&mut self) {
        self.stream.write(&[PLAYER_IS_FOUND]).unwrap();
    }

    #[inline]
    pub(crate) fn send_move(&mut self, y: u8, x: u8) {
        self.stream.write(&[PLAYER_MOVED, y, x]).unwrap();
    }

    #[inline]
    pub(crate) fn read_command(&mut self) -> u8 {
        let mut data = [0];
        self.stream.read_exact(&mut data).unwrap();
        unsafe { *data.get_unchecked(0) }
    }

    #[inline]
    pub(crate) fn read_role(&mut self) -> u8 {
        let mut data = [0];
        self.stream.read_exact(&mut data).unwrap();
        unsafe { *data.get_unchecked(0) }
    }

    #[inline]
    pub(crate) fn read_table(&mut self) -> [[u8; 3]; 3] {
        let mut data = [0; 9];
        self.stream.read_exact(&mut data).unwrap();

        let mut table = [[0; 3]; 3];
        let mut iter = data.iter();

        (0..=3).for_each(|i| {
            (0..=3).for_each(|q| unsafe {
                *table.get_unchecked_mut(i).get_unchecked_mut(q) = *iter.next().unwrap()
            })
        });

        table
    }
}
